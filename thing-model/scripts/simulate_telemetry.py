#!/usr/bin/env python3
from __future__ import annotations

import argparse
import getpass
import json
import os
import random
import sys
import time
import uuid
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
            self, token: str, profile_id: str, device_name: str) -> Dict[str, Any]:
        response = self.session.post(
            f"{self.tb_url}/api/device",
            headers=self._auth_headers(token),
            json={
                "name": device_name,
                "type": "default",
                "deviceProfileId": {
                    "entityType": "DEVICE_PROFILE",
                    "id": profile_id,
                },
            },
            timeout=self.timeout,
        )
        self._check(response)
        return response.json()

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

    def send_telemetry(self, access_token: str, payload: Dict[str, Any]) -> None:
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

    def telemetry(self) -> Dict[str, Any]:
        values: Dict[str, Any] = {}
        for prop in self.properties:
            value = self._property_value(prop)
            if value is not self._SKIP:
                values[prop["id"]] = value
        return values

    def event(self) -> Dict[str, Any]:
        values = self.telemetry()
        event = self.rng.choice(self.events) if self.events else None
        if event:
            event_id = event.get("id", "event")
            event_type = event.get("type") or self._random_event_type()
        else:
            event_id = f"evt-{uuid.uuid4().hex[:8]}"
            event_type = self._random_event_type()
        values["event_id"] = event_id
        values["event_type"] = event_type
        values["event_ts"] = int(time.time() * 1000)
        return values

    def invalid(self) -> Dict[str, Any]:
        values = self.telemetry()
        target = next(
            (prop for prop in self.properties
             if prop.get("dataType") in ("int", "float", "timestamp", "enum")),
            None,
        )
        if target:
            maximum = target.get("max")
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

    def _property_value(self, prop: Dict[str, Any]) -> Any:
        data_type = str(prop.get("dataType") or "string").lower()
        if data_type == "int":
            return self._random_int(prop)
        if data_type == "float":
            return self._random_float(prop)
        if data_type == "bool":
            return self.rng.choice([True, False])
        if data_type in ("enum", "enum_string"):
            return self._enum_value(prop)
        if data_type == "timestamp":
            return int(time.time())
        if data_type == "struct":
            print(
                f"skip struct property: {prop.get('id')}",
                file=sys.stderr,
            )
            return self._SKIP
        return self._string_value(prop)

    def _random_int(self, prop: Dict[str, Any]) -> int:
        minimum = prop.get("min")
        maximum = prop.get("max")
        if minimum is not None and maximum is not None:
            return self.rng.randint(int(minimum), int(maximum))
        if minimum is not None:
            return int(minimum) + self.rng.randint(0, 100)
        if maximum is not None:
            return int(maximum) - self.rng.randint(0, 100)
        return self.rng.randint(0, 100)

    def _random_float(self, prop: Dict[str, Any]) -> float:
        minimum = prop.get("min")
        maximum = prop.get("max")
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

    def _enum_value(self, prop: Dict[str, Any]) -> Any:
        mapping = prop.get("mapping")
        if isinstance(mapping, dict) and mapping:
            return self.rng.choice(list(mapping.keys()))
        if isinstance(mapping, list) and mapping:
            item = self.rng.choice(mapping)
            if isinstance(item, dict):
                return item.get("key", item.get("id", 0))
            return item
        return 0

    def _random_event_type(self) -> str:
        return self.rng.choice(["info", "alert", "fault"])


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
        help="message count; 0 means unlimited",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="random seed",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="print payloads without creating a device",
    )
    parser.add_argument(
        "--keep-device",
        action="store_true",
        help="keep the generated test device on exit",
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
        return ["telemetry", "event", "invalid"]
    return [mode]


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
        properties = catalog.get("properties") or []
        events = catalog.get("events") or []
        if not properties:
            raise ApiError(f"no properties found for product {args.product_id}")

        generator = TelemetryGenerator(properties, events, rng)
        sequence = mode_sequence(args.mode)

        if args.dry_run:
            for index in range(args.count or len(sequence)):
                kind = sequence[index % len(sequence)]
                payload = getattr(generator, kind)()
                print(f"[{kind}] {json.dumps(payload, ensure_ascii=False)}")
            return 0

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
            "tsl_version": product_info.get("tslVersion") or "1.0",
            "tenant_id": tenant_id,
            "device_uuid": device_id,
            "site_code": args.site_code,
            "product_id": args.product_id,
        })
        print(
            f"device={device_name} id={device_id} product={args.product_id} "
            f"mode={args.mode} interval={args.interval}s")

        index = 0
        while args.count <= 0 or index < args.count:
            kind = sequence[index % len(sequence)]
            payload = getattr(generator, kind)()
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
