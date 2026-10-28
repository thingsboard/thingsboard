#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import getpass
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List

from simulate_telemetry import ApiError, ThingsBoardClient, find_tsl_schema

CSV_FIELDS = [
    "product_id",
    "device_name",
    "device_id",
    "access_token",
    "tsl_version",
    "profile_id",
    "site_code",
    "status",
    "error",
    "created_at",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Register ThingsBoard devices for a thing-model product.")
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
        default=os.getenv("PRODUCT_ID"),
        help="thing-model product id; defaults to PRODUCT_ID",
    )
    parser.add_argument(
        "--name",
        action="append",
        default=[],
        help="device name; repeat for multiple devices",
    )
    parser.add_argument(
        "--prefix",
        default=None,
        help="batch device name prefix",
    )
    parser.add_argument(
        "--count",
        type=int,
        default=0,
        help="batch device count",
    )
    parser.add_argument(
        "--start-index",
        type=int,
        default=1,
        help="first batch index, default 1",
    )
    parser.add_argument(
        "--label-prefix",
        default="",
        help="optional device label prefix",
    )
    parser.add_argument(
        "--site-code",
        default=os.getenv("SITE_CODE", "SIM"),
        help="site_code server attribute",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="CSV output path",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=10.0,
        help="HTTP timeout in seconds",
    )
    return parser.parse_args()


def build_device_names(args: argparse.Namespace) -> List[str]:
    if args.name:
        if args.prefix or args.count:
            raise ApiError("use either --name or --prefix/--count, not both")
        names = list(args.name)
    else:
        if not args.prefix or args.count <= 0:
            raise ApiError("either --name or --prefix with --count is required")
        base = args.prefix.rstrip("-")
        if not base:
            raise ApiError("--prefix must contain a non-dash character")
        width = max(4, len(str(args.start_index + args.count - 1)))
        names = [
            f"{base}-{args.start_index + index:0{width}d}"
            for index in range(args.count)
        ]
    if not names:
        raise ApiError("no device names to register")
    if len(set(names)) != len(names):
        raise ApiError("duplicate device names")
    return names


def resolve_output_path(args: argparse.Namespace) -> Path:
    if args.output:
        path = Path(args.output)
    else:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        path = Path.cwd() / f"device-credentials-{args.product_id}-{stamp}.csv"
    if path.exists():
        raise ApiError(f"output file already exists: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def log_row(
        writer: csv.DictWriter,
        stream: Any,
        product_id: str,
        device_name: str,
        tsl_version: str,
        profile_id: str,
        site_code: str,
        device_id: str = "",
        access_token: str = "",
        status: str = "",
        error: str = "") -> None:
    writer.writerow({
        "product_id": product_id,
        "device_name": device_name,
        "device_id": device_id,
        "access_token": access_token,
        "tsl_version": tsl_version,
        "profile_id": profile_id,
        "site_code": site_code,
        "status": status,
        "error": error,
        "created_at": datetime.now(timezone.utc).isoformat(),
    })
    stream.flush()


def main() -> int:
    args = parse_args()
    if not args.product_id:
        print("error: --product-id is required (or set PRODUCT_ID)", file=sys.stderr)
        return 1

    password = args.password
    if not password:
        password = getpass.getpass(
            f"ThingsBoard password for {args.username}: ")

    try:
        names = build_device_names(args)
        output_path = resolve_output_path(args)
    except ApiError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    client = ThingsBoardClient(args.tb_url, args.thingmodel_url, args.timeout)
    created = 0
    skipped = 0
    failed = 0

    try:
        token = client.login(args.username, password)
        product = client.get_product(token, args.product_id)
        product_info = product.get("product") or {}
        profile_id = product_info.get("tbProfileId")
        asset_id = product_info.get("tbAssetId")
        tenant_id = product_info.get("tenantId")
        if not profile_id or not asset_id or not tenant_id:
            raise ApiError(
                "product is not synced: missing tbProfileId/tbAssetId/tenantId; "
                "run product sync first")
        tsl_version = product_info.get("tslVersion") or "1.0"
        tsl_schema = find_tsl_schema(
            client.get_asset_attributes(token, asset_id))
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    with output_path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=CSV_FIELDS)
        writer.writeheader()

        for device_name in names:
            device_id = ""
            access_token = ""
            try:
                existing = client.find_device_by_name(token, device_name)
                if existing:
                    device_id = (existing.get("id") or {}).get("id", "")
                    log_row(
                        writer, stream, args.product_id, device_name,
                        tsl_version, profile_id, args.site_code,
                        device_id=device_id, status="skipped")
                    skipped += 1
                    print(f"skipped {device_name} ({device_id})")
                    continue

                label = f"{args.label_prefix}{device_name}" if args.label_prefix else None
                device = client.create_device(
                    token, profile_id, device_name, label=label)
                device_id = (device.get("id") or {}).get("id")
                if not device_id:
                    raise ApiError("ThingsBoard did not return a device id")
                credentials = client.get_credentials(token, device_id)
                access_token = credentials.get("credentialsId")
                if not access_token:
                    raise ApiError("ThingsBoard did not return an access token")
                client.set_server_attributes(token, device_id, {
                    "tsl_schema": tsl_schema,
                    "tsl_version": tsl_version,
                    "tenant_id": tenant_id,
                    "device_uuid": device_id,
                    "site_code": args.site_code,
                    "product_id": args.product_id,
                })
                log_row(
                    writer, stream, args.product_id, device_name,
                    tsl_version, profile_id, args.site_code,
                    device_id=device_id, access_token=access_token,
                    status="created")
                created += 1
                print(f"created {device_name} ({device_id})")
            except Exception as exc:
                log_row(
                    writer, stream, args.product_id, device_name,
                    tsl_version, profile_id, args.site_code,
                    device_id=device_id, access_token=access_token,
                    status="failed", error=str(exc))
                failed += 1
                print(f"failed {device_name}: {exc}", file=sys.stderr)

    print(
        f"summary created={created} skipped={skipped} failed={failed} "
        f"output={output_path}")
    print(
        "warning: the CSV contains Access Tokens; keep it secure",
        file=sys.stderr)
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
