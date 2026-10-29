#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import getpass
import json
import os
import random
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional

import requests


class ApiError(RuntimeError):
    pass


class ThingsBoardClient:
    def __init__(self, tb_url: str, thingmodel_url: str, timeout: float) -> None:
        self.tb_url = tb_url.rstrip("/")
        self.thingmodel_url = thingmodel_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()

    def login(self, username: str, password: str) -> str:
        response = self.session.post(
            f"{self.tb_url}/api/auth/login",
            json={"username": username, "password": password},
            timeout=self.timeout,
        )
        self._check(response)
        return response.json()["token"]

    def get_product(self, token: str, product_id: str) -> Dict[str, Any]:
        response = self.session.get(
            f"{self.thingmodel_url}/api/products/{product_id}",
            headers=self._auth_headers(token),
            timeout=self.timeout,
        )
        self._check(response)
        return response.json()

    def get_asset_attributes(self, token: str, asset_id: str) -> List[Dict[str, Any]]:
        response = self.session.get(
            f"{self.tb_url}/api/plugins/telemetry/ASSET/{asset_id}/values/attributes/SERVER_SCOPE",
            headers=self._auth_headers(token),
            timeout=self.timeout,
        )
        self._check(response)
        return response.json()

    def create_device(
            self,
            token: str,
            profile_id: str,
            device_name: str,
            label: Optional[str] = None) -> Dict[str, Any]:
        payload: Dict[str, Any] = {
            "name": device_name,
            "type": "default",
            "deviceProfileId": {
                "entityType": "DEVICE_PROFILE",
                "id": profile_id,
            },
        }
        if label:
            payload["label"] = label
        response = self.session.post(
            f"{self.tb_url}/api/device",
            headers=self._auth_headers(token),
            json=payload,
            timeout=self.timeout,
        )
        self._check(response)
        return response.json()

    def find_device_by_name(
            self, token: str, device_name: str) -> Optional[Dict[str, Any]]:
        response = self.session.get(
            f"{self.tb_url}/api/tenant/devices",
            params={"deviceName": device_name},
            headers=self._auth_headers(token),
            timeout=self.timeout,
        )
        if response.status_code == 404:
            return None
        self._check(response)
        return response.json() or None

    def get_credentials(self, token: str, device_id: str) -> Dict[str, Any]:
        response = self.session.get(
            f"{self.tb_url}/api/device/{device_id}/credentials",
            headers=self._auth_headers(token),
            timeout=self.timeout,
        )
        self._check(response)
        return response.json()

    def set_server_attributes(
            self, token: str, device_id: str, attributes: Dict[str, Any]) -> None:
        response = self.session.post(
            f"{self.tb_url}/api/plugins/telemetry/DEVICE/{device_id}/attributes/SERVER_SCOPE",
            headers=self._auth_headers(token),
            json=attributes,
            timeout=self.timeout,
        )
        self._check(response)

    def delete_device(self, token: str, device_id: str) -> None:
        response = self.session.delete(
            f"{self.tb_url}/api/device/{device_id}",
            headers=self._auth_headers(token),
            timeout=self.timeout,
        )
        if response.status_code != 404:
            self._check(response)

    def send_telemetry(self, access_token: str, payload: Any) -> None:
        response = self.session.post(
            f"{self.tb_url}/api/v1/{access_token}/telemetry",
            json=payload,
            timeout=self.timeout,
        )
        self._check(response)

    def _auth_headers(self, token: str) -> Dict[str, str]:
        return {"X-Authorization": f"Bearer {token}"}

    def _check(self, response: requests.Response) -> None:
        if response.status_code >= 400:
            raise ApiError(
                f"HTTP {response.status_code} {response.url}: {response.text[:500]}")


class TelemetryGenerator:
    _SKIP = object()

    def __init__(
            self,
            properties: Iterable[Dict[str, Any]],
            events: Iterable[Dict[str, Any]],
            rng: random.Random) -> None:
        self.properties = list(properties or [])
        self.events = list(events or [])
        self.rng = rng

    @staticmethod
    def _definition(item: Dict[str, Any]) -> Dict[str, Any]:
        define = item.get("define")
        return define if isinstance(define, dict) else item

    @classmethod
    def _type(cls, item: Dict[str, Any]) -> str:
        define = cls._definition(item)
        return str(define.get("type") or item.get("dataType") or "string").lower()

    def telemetry(self) -> Dict[str, Any]:
        values: Dict[str, Any] = {}
        for prop in self.properties:
            value = self._property_value(prop)
            if value is not self._SKIP:
                values[prop["id"]] = value
        return values

    def event(self, event: Optional[Dict[str, Any]] = None,
              ts: Optional[int] = None) -> Dict[str, Any]:
        selected = event
        if selected is None and self.events:
            selected = self.rng.choice(self.events)
        return self._event_values(selected, ts)

    def event_batch(self, count: int = 4) -> Dict[str, Any]:
        base_ts = int(time.time() * 1000)
        event = self.rng.choice(self.events) if self.events else None
        return {
            "events": [
                self._event_values(event, base_ts + index)
                for index in range(max(1, count))
            ]
        }

    def payload(self, kind: str, event_array: bool = True,
                events_per_packet: int = 4) -> Any:
        if kind == "event":
            if event_array:
                return self.event_batch(events_per_packet)
            return self.event()
        return getattr(self, kind)()

    def _event_values(self, event: Optional[Dict[str, Any]],
                      ts: Optional[int] = None) -> Dict[str, Any]:
        values: Dict[str, Any] = {}
        if event:
            event_id = event.get("id", "event")
            event_type = event.get("type") or self._random_event_type()
            for param in event.get("params") or []:
                value = self._property_value(param)
                if value is not self._SKIP:
                    values[param["id"]] = value
        else:
            event_id = f"evt-{uuid.uuid4().hex[:8]}"
            event_type = self._random_event_type()
        values["event_uid"] = str(uuid.uuid4())
        values["event_id"] = event_id
        values["event_type"] = event_type
        values["event_ts"] = int(time.time() * 1000) if ts is None else ts
        return values

    def invalid(self) -> Dict[str, Any]:
        values = self.telemetry()
        target = next(
            (prop for prop in self.properties
             if self._type(prop) in ("int", "float", "timestamp", "enum")),
            None,
        )
        if target:
            define = self._definition(target)
            maximum = define.get("max")
            if maximum is not None:
                values[target["id"]] = maximum + 100
            else:
                values[target["id"]] = "not-a-number"
        elif self.properties:
            values[self.properties[0]["id"]] = {"invalid": True}
        else:
            values["__sim_invalid__"] = {"invalid": True}
        values["__sim_unknown__"] = "x"
        return values

    def _property_value(self, prop: Dict[str, Any], nested: bool = False) -> Any:
        define = self._definition(prop)
        data_type = self._type(prop)
        if data_type == "int":
            return self._random_int(define)
        if data_type == "float":
            return self._random_float(define)
        if data_type == "bool":
            return self.rng.choice([True, False])
        if data_type in ("enum", "enum_string"):
            return self._enum_value(data_type, define)
        if data_type == "timestamp":
            return int(time.time())
        if data_type == "struct":
            if nested:
                print(
                    f"skip nested struct field: {prop.get('id')}",
                    file=sys.stderr,
                )
                return self._SKIP
            return self._struct_value(define)
        return self._string_value(prop)

    def _struct_value(self, define: Dict[str, Any]) -> Dict[str, Any]:
        specs = define.get("specs") or define.get("fields")
        if isinstance(specs, dict):
            fields = specs.items()
        elif isinstance(specs, list):
            fields = (
                (item.get("id"), item)
                for item in specs
                if isinstance(item, dict) and item.get("id")
            )
        else:
            fields = ()
        result: Dict[str, Any] = {}
        for field_id, field in fields:
            value = self._property_value(field, nested=True)
            if value is not self._SKIP:
                result[field_id] = value
        return result

    def _random_int(self, define: Dict[str, Any]) -> int:
        minimum = define.get("min")
        maximum = define.get("max")
        if minimum is not None and maximum is not None:
            return self.rng.randint(int(minimum), int(maximum))
        if minimum is not None:
            return int(minimum) + self.rng.randint(0, 100)
        if maximum is not None:
            return int(maximum) - self.rng.randint(0, 100)
        return self.rng.randint(0, 100)

    def _random_float(self, define: Dict[str, Any]) -> float:
        minimum = define.get("min")
        maximum = define.get("max")
        if minimum is not None and maximum is not None:
            value = self.rng.uniform(float(minimum), float(maximum))
        elif minimum is not None:
            value = float(minimum) + self.rng.uniform(0, 100)
        elif maximum is not None:
            value = float(maximum) - self.rng.uniform(0, 100)
        else:
            value = self.rng.uniform(0, 100)
        return round(value, 3)

    def _string_value(self, prop: Dict[str, Any]) -> str:
        property_id = str(prop.get("id") or "value")
        if "bssid" in property_id.lower() or "mac" in property_id.lower():
            return ":".join(
                f"{self.rng.randint(0, 255):02X}" for _ in range(6))
        return f"sim-{uuid.uuid4().hex[:8]}"

    def _enum_value(self, data_type: str, define: Dict[str, Any]) -> Any:
        mapping = define.get("mapping")
        if isinstance(mapping, dict) and mapping:
            key = self.rng.choice(list(mapping.keys()))
        elif isinstance(mapping, list) and mapping:
            item = self.rng.choice(mapping)
            if isinstance(item, dict):
                key = item.get("key", item.get("id", 0))
            else:
                key = item
        else:
            return 0 if data_type == "enum" else ""
        if data_type == "enum":
            try:
                return int(key)
            except (TypeError, ValueError):
                return key
        return str(key)

    def _random_event_type(self) -> str:
        return self.rng.choice(["info", "alert", "fault"])


def load_tsl(path: str) -> Dict[str, Any]:
    tsl_path = Path(path)
    if not tsl_path.is_file():
        raise ApiError(f"TSL file not found: {tsl_path}")
    with tsl_path.open(encoding="utf-8") as stream:
        tsl = json.load(stream)
    if not isinstance(tsl, dict):
        raise ApiError(f"TSL file must be a JSON object: {tsl_path}")
    return tsl


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Simulate thing-model telemetry for ThingsBoard and Kafka.")
    parser.add_argument(
        "--tb-url",
        default=os.getenv("TB_URL", "http://localhost:18080"),
        help="ThingsBoard base URL",
    )
    parser.add_argument(
        "--thingmodel-url",
        default=os.getenv("THINGMODEL_URL", "http://localhost:8090"),
        help="thing-model base URL",
    )
    parser.add_argument(
        "--username",
        default=os.getenv("TB_USERNAME", "tenant@thingsboard.org"),
        help="ThingsBoard tenant admin username",
    )
    parser.add_argument(
        "--password",
        default=os.getenv("TB_PASSWORD"),
        help="ThingsBoard password; defaults to TB_PASSWORD or prompt",
    )
    parser.add_argument(
        "--product-id",
        default=os.getenv("PRODUCT_ID", "9HYN9PJNOY"),
        help="thing-model product id",
    )
    parser.add_argument(
        "--tsl",
        default=None,
        help="local TSL JSON file; when set, use its properties and events",
    )
    parser.add_argument(
        "--device-name",
        default=None,
        help="test device name; defaults to sim-<product>-<timestamp>",
    )
    parser.add_argument(
        "--site-code",
        default=os.getenv("SITE_CODE", "SIM"),
        help="site_code server attribute",
    )
    parser.add_argument(
        "--mode",
        choices=["telemetry", "event", "invalid", "mixed"],
        default="mixed",
        help="message mode",
    )
    parser.add_argument(
        "--interval",
        type=float,
        default=5.0,
        help="seconds between messages",
    )
    parser.add_argument(
        "--count",
        type=int,
        default=0,
        help="message count per device; 0 means unlimited in single-device mode",
    )
    parser.add_argument(
        "--devices-csv",
        default=None,
        help="registration CSV; send to all devices with access tokens",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=10,
        help="parallel workers in --devices-csv mode",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="random seed",
    )
    parser.add_argument(
        "--keep-device",
        action="store_true",
        help="keep the generated test device on exit",
    )
    parser.add_argument(
        "--single-event",
        action="store_true",
        help="send one random event object instead of an event array",
    )
    parser.add_argument(
        "--events-per-packet",
        type=int,
        default=4,
        help="number of same-type events per array packet",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=10.0,
        help="HTTP timeout in seconds",
    )
    return parser.parse_args()


def find_tsl_schema(attributes: Iterable[Dict[str, Any]]) -> str:
    for attribute in attributes:
        if attribute.get("key") == "tsl_schema" and attribute.get("value"):
            return str(attribute["value"])
    raise ApiError("tsl_schema not found on product asset; run product sync first")


def mode_sequence(mode: str) -> List[str]:
    if mode == "mixed":
        return ["telemetry", "event"]
    return [mode]


def load_device_credentials(path: str, product_id: str) -> List[Dict[str, str]]:
    csv_path = Path(path)
    if not csv_path.is_file():
        raise ApiError(f"devices CSV not found: {csv_path}")
    devices: List[Dict[str, str]] = []
    with csv_path.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream)
        for row in reader:
            access_token = (row.get("access_token") or "").strip()
            if not access_token:
                continue
            row_product = (row.get("product_id") or "").strip()
            if row_product and row_product != product_id:
                raise ApiError(
                    f"CSV product_id {row_product} does not match "
                    f"--product-id {product_id}")
            devices.append(row)
    if not devices:
        raise ApiError(f"no devices with access tokens found in {csv_path}")
    return devices


def send_payloads(
        client: ThingsBoardClient,
        access_token: str,
        generator: TelemetryGenerator,
        sequence: List[str],
        count: int,
        interval: float,
        device_name: str,
        event_array: bool = True,
        events_per_packet: int = 4) -> Dict[str, int]:
    sent = {"telemetry": 0, "event": 0, "invalid": 0}
    errors = 0
    for index in range(count):
        kind = sequence[index % len(sequence)]
        payload = generator.payload(kind, event_array, events_per_packet)
        try:
            client.send_telemetry(access_token, payload)
            sent[kind] += 1
        except Exception as exc:
            errors += 1
            print(f"  {device_name} [{kind}] ERROR {exc}", file=sys.stderr)
        if interval > 0 and index + 1 < count:
            time.sleep(interval)
    return {"telemetry": sent["telemetry"], "event": sent["event"],
            "invalid": sent["invalid"], "errors": errors}


def run_batch(
        args: argparse.Namespace,
        devices: List[Dict[str, str]],
        properties: Iterable[Dict[str, Any]],
        events: Iterable[Dict[str, Any]],
        source: str) -> int:
    if args.count <= 0:
        raise ApiError("--devices-csv mode requires --count > 0")
    workers = max(1, args.workers)
    sequence = mode_sequence(args.mode)
    event_array = not args.single_event
    events_per_packet = args.events_per_packet
    total_sent = {"telemetry": 0, "event": 0, "invalid": 0}
    total_errors = 0

    print(
        f"devices={len(devices)} source={source} mode={args.mode} "
        f"count={args.count} workers={workers}")

    def task(item: Any) -> Any:
        index, row = item
        device_name = row.get("device_name") or row.get("device_id") or "device"
        seed = args.seed + index if args.seed is not None else None
        generator = TelemetryGenerator(properties, events, random.Random(seed))
        client = ThingsBoardClient(args.tb_url, args.thingmodel_url, args.timeout)
        result = send_payloads(
            client, row["access_token"], generator, sequence,
            args.count, args.interval, device_name, event_array,
            events_per_packet)
        return device_name, result

    with ThreadPoolExecutor(max_workers=workers) as executor:
        futures = [executor.submit(task, item) for item in enumerate(devices)]
        for future in as_completed(futures):
            try:
                device_name, result = future.result()
                total_sent["telemetry"] += result["telemetry"]
                total_sent["event"] += result["event"]
                total_sent["invalid"] += result["invalid"]
                total_errors += result["errors"]
                print(
                    f"{device_name}: telemetry={result['telemetry']} "
                    f"event={result['event']} invalid={result['invalid']} "
                    f"errors={result['errors']}")
            except Exception as exc:
                total_errors += 1
                print(f"device task failed: {exc}", file=sys.stderr)

    print(
        f"summary devices={len(devices)} telemetry={total_sent['telemetry']} "
        f"event={total_sent['event']} invalid={total_sent['invalid']} "
        f"errors={total_errors}")
    return 0 if total_errors == 0 else 1


def main() -> int:
    args = parse_args()
    password = args.password
    if not password:
        password = getpass.getpass(
            f"ThingsBoard password for {args.username}: ")

    rng = random.Random(args.seed)
    client = ThingsBoardClient(args.tb_url, args.thingmodel_url, args.timeout)
    device_id: Optional[str] = None
    token: Optional[str] = None
    access_token: Optional[str] = None
    sent = {"telemetry": 0, "event": 0, "invalid": 0}
    errors = 0

    try:
        token = client.login(args.username, password)
        product = client.get_product(token, args.product_id)
        product_info = product.get("product") or {}
        catalog = product.get("catalog") or {}

        if args.tsl:
            tsl = load_tsl(args.tsl)
            tsl_profile = tsl.get("profile") or {}
            tsl_product_id = tsl_profile.get("ProductId") or tsl_profile.get("productId")
            if tsl_product_id and tsl_product_id != args.product_id:
                raise ApiError(
                    f"TSL ProductId {tsl_product_id} does not match "
                    f"--product-id {args.product_id}")
            properties = tsl.get("properties") or []
            events = tsl.get("events") or []
            tsl_version = tsl.get("version") or product_info.get("tslVersion") or "1.0"
            source = f"tsl={args.tsl}"
        else:
            properties = catalog.get("properties") or []
            events = catalog.get("events") or []
            tsl_version = product_info.get("tslVersion") or "1.0"
            source = "catalog"

        if not properties:
            raise ApiError(f"no properties found for product {args.product_id}")

        if args.devices_csv:
            devices = load_device_credentials(args.devices_csv, args.product_id)
            return run_batch(args, devices, properties, events, source)

        generator = TelemetryGenerator(properties, events, rng)
        sequence = mode_sequence(args.mode)

        profile_id = product_info.get("tbProfileId")
        asset_id = product_info.get("tbAssetId")
        tenant_id = product_info.get("tenantId")
        if not profile_id or not asset_id or not tenant_id:
            raise ApiError("product is missing tbProfileId, tbAssetId or tenantId")

        tsl_schema = find_tsl_schema(
            client.get_asset_attributes(token, asset_id))
        device_name = args.device_name or (
            f"sim-{args.product_id}-{int(time.time())}-{uuid.uuid4().hex[:4]}")
        device = client.create_device(token, profile_id, device_name)
        device_id = device["id"]["id"]
        credentials = client.get_credentials(token, device_id)
        access_token = credentials["credentialsId"]
        client.set_server_attributes(token, device_id, {
            "tsl_schema": tsl_schema,
            "tsl_version": tsl_version,
            "tenant_id": tenant_id,
            "device_uuid": device_id,
            "site_code": args.site_code,
            "product_id": args.product_id,
        })
        print(
            f"device={device_name} id={device_id} product={args.product_id} "
            f"source={source} mode={args.mode} interval={args.interval}s")

        index = 0
        while args.count <= 0 or index < args.count:
            kind = sequence[index % len(sequence)]
            payload = generator.payload(
                kind, not args.single_event, args.events_per_packet)
            try:
                client.send_telemetry(access_token, payload)
                sent[kind] += 1
                print(f"[{kind}] 200 {json.dumps(payload, ensure_ascii=False)}")
            except Exception as exc:
                errors += 1
                print(f"[{kind}] ERROR {exc}", file=sys.stderr)
            index += 1
            if args.count <= 0 or index < args.count:
                time.sleep(args.interval)
    except KeyboardInterrupt:
        print("\ninterrupted", file=sys.stderr)
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    finally:
        if not args.devices_csv:
            print(
                f"summary telemetry={sent['telemetry']} event={sent['event']} "
                f"invalid={sent['invalid']} errors={errors}")
            if device_id and not args.keep_device:
                try:
                    client.delete_device(token, device_id)
                    print(f"deleted device {device_id}")
                except Exception as exc:
                    print(f"failed to delete device {device_id}: {exc}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
