#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

NUMERIC_TYPES = {"int", "float", "timestamp", "enum"}
TYPE_MAP = {"stringenum": "enum_string"}
DEFINE_SKIP_KEYS = {
    "id",
    "name",
    "desc",
    "required",
    "specs",
    "fields",
    "dataType",
}


def number(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    text = value.strip()
    try:
        if any(ch in text.lower() for ch in (".", "e")):
            return float(text)
        return int(text)
    except ValueError:
        return value


def convert_specs(specs: Any, keep_string_ranges: bool) -> Any:
    if isinstance(specs, list):
        result: dict[str, Any] = {}
        for item in specs:
            if not isinstance(item, dict):
                continue
            field_id = item.get("id")
            if not field_id:
                continue
            field = convert_define(
                item.get("dataType", item.get("define", item)),
                keep_string_ranges,
            )
            if item.get("name"):
                field["name"] = item["name"]
            if item.get("desc"):
                field["desc"] = item["desc"]
            if "required" in item:
                field["required"] = item["required"]
            result[field_id] = field
        return result
    if isinstance(specs, dict):
        result = {}
        for field_id, item in specs.items():
            if isinstance(item, dict):
                field = convert_define(
                    item.get("dataType", item.get("define", item)),
                    keep_string_ranges,
                )
                if item.get("name"):
                    field["name"] = item["name"]
                if item.get("desc"):
                    field["desc"] = item["desc"]
                if "required" in item:
                    field["required"] = item["required"]
                result[field_id] = field
            else:
                result[field_id] = item
        return result
    return specs


def convert_define(define: Any, keep_string_ranges: bool = False) -> Any:
    if not isinstance(define, dict):
        return define
    result: dict[str, Any] = {}
    for key, value in define.items():
        if key in DEFINE_SKIP_KEYS:
            continue
        if key in ("type", "min", "max", "start", "step", "unit", "mapping"):
            continue
        result[key] = value

    type_name = TYPE_MAP.get(define.get("type"), define.get("type"))
    if type_name is not None:
        result["type"] = type_name

    numeric = type_name in NUMERIC_TYPES
    for key in ("min", "max", "start", "step"):
        if key in define and (numeric or keep_string_ranges):
            result[key] = number(define[key])

    unit = define.get("unit")
    if unit:
        result["unit"] = unit
    if "mapping" in define:
        result["mapping"] = define["mapping"]

    specs = define.get("specs", define.get("fields"))
    if specs is not None:
        result["specs"] = convert_specs(specs, keep_string_ranges)
    return result


def convert_property(prop: dict[str, Any], keep_string_ranges: bool) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key in ("id", "name", "desc", "mode", "required"):
        if key in prop:
            result[key] = prop[key]
    result["define"] = convert_define(prop.get("define", {}), keep_string_ranges)
    if "mapping" in prop:
        result["mapping"] = prop["mapping"]
    return result


def convert_param(param: dict[str, Any], keep_string_ranges: bool) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key in ("id", "name", "desc"):
        if key in param:
            result[key] = param[key]
    result["define"] = convert_define(param.get("define", {}), keep_string_ranges)
    return result


def convert_event(event: dict[str, Any], keep_string_ranges: bool) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key in ("id", "name", "desc", "type", "required"):
        if key in event:
            result[key] = event[key]
    if "params" in event:
        result["params"] = [
            convert_param(param, keep_string_ranges)
            for param in event["params"]
        ]
    return result


def convert_action(action: dict[str, Any], keep_string_ranges: bool) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key in ("id", "name", "desc"):
        if key in action:
            result[key] = action[key]
    for key in ("input", "output"):
        if key in action:
            result[key] = [
                convert_param(param, keep_string_ranges)
                for param in action[key]
            ]
    return result


def flatten_properties(properties: list[dict[str, Any]]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for prop in properties:
        define = prop.get("define")
        specs = define.get("specs") if isinstance(define, dict) else None
        if (
            isinstance(define, dict)
            and define.get("type") == "struct"
            and isinstance(specs, dict)
        ):
            for field_id, field in specs.items():
                if not isinstance(field, dict):
                    continue
                item: dict[str, Any] = {
                    "id": f"{prop.get('id', 'struct')}_{field_id}",
                    "name": f"{prop.get('name', '')} {field.get('name', field_id)}".strip(),
                    "mode": prop.get("mode", "r"),
                    "required": prop.get("required", False),
                    "define": {
                        key: value
                        for key, value in field.items()
                        if key not in ("name", "desc", "required")
                    },
                }
                if "desc" in prop:
                    item["desc"] = prop["desc"]
                result.append(item)
        else:
            result.append(prop)
    return result


def convert_document(
    source: dict[str, Any],
    keep_string_ranges: bool = False,
    flatten_structs: bool = False,
    product_id: str | None = None,
) -> dict[str, Any]:
    if not isinstance(source, dict):
        raise ValueError("Tencent TSL must be a JSON object")

    profile = dict(source.get("profile") or {})
    if product_id:
        profile["ProductId"] = product_id

    properties = [
        convert_property(prop, keep_string_ranges)
        for prop in source.get("properties", [])
    ]
    if flatten_structs:
        properties = flatten_properties(properties)

    return {
        "version": source.get("version", "1.0"),
        "profile": profile,
        "properties": properties,
        "events": [
            convert_event(event, keep_string_ranges)
            for event in source.get("events", [])
        ],
        "actions": [
            convert_action(action, keep_string_ranges)
            for action in source.get("actions", [])
        ],
    }


def write_document(
    output: Path,
    converted: dict[str, Any],
    indent: int,
) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="\n") as stream:
        json.dump(converted, stream, ensure_ascii=False, indent=indent)
        stream.write("\n")


def process_file(
    input_path: Path,
    output_path: Path,
    keep_string_ranges: bool,
    flatten_structs: bool,
    product_id: str | None,
    indent: int,
) -> None:
    with input_path.open(encoding="utf-8") as stream:
        source = json.load(stream)
    converted = convert_document(
        source,
        keep_string_ranges=keep_string_ranges,
        flatten_structs=flatten_structs,
        product_id=product_id,
    )
    write_document(output_path, converted, indent)
    print(f"{input_path} -> {output_path}")


def process_directory(
    input_dir: Path,
    output_dir: Path,
    keep_string_ranges: bool,
    flatten_structs: bool,
    product_id: str | None,
    indent: int,
) -> None:
    files = [
        path for path in sorted(input_dir.glob("*.json"))
        if not path.name.endswith(".tsl.json")
    ]
    if not files:
        raise ValueError(f"No JSON files found in {input_dir}")
    for input_path in files:
        output_path = output_dir / f"{input_path.stem}.tsl.json"
        process_file(
            input_path,
            output_path,
            keep_string_ranges,
            flatten_structs,
            product_id,
            indent,
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert Tencent IoT TSL to thing-model TSL."
    )
    parser.add_argument("input", type=Path, help="Input JSON file or directory")
    parser.add_argument("-o", "--output", type=Path, help="Output file path")
    parser.add_argument(
        "--output-dir",
        type=Path,
        help="Output directory for batch conversion",
    )
    parser.add_argument(
        "--keep-string-ranges",
        action="store_true",
        help="Keep min/max/start/step on string fields",
    )
    parser.add_argument(
        "--flatten-structs",
        action="store_true",
        help="Flatten struct properties using underscore-separated ids",
    )
    parser.add_argument("--product-id", help="Override profile.ProductId")
    parser.add_argument("--indent", type=int, default=2, help="Output indent")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        if args.input.is_dir():
            if args.output:
                raise ValueError("--output is only valid for a single input file")
            output_dir = args.output_dir or (args.input / "converted")
            process_directory(
                args.input,
                output_dir,
                args.keep_string_ranges,
                args.flatten_structs,
                args.product_id,
                args.indent,
            )
        else:
            if args.output and args.output_dir:
                raise ValueError("Use either --output or --output-dir")
            output = args.output
            if output is None:
                if args.output_dir:
                    output = args.output_dir / f"{args.input.stem}.tsl.json"
                else:
                    output = args.input.parent / f"{args.input.stem}.tsl.json"
            process_file(
                args.input,
                output,
                args.keep_string_ranges,
                args.flatten_structs,
                args.product_id,
                args.indent,
            )
        return 0
    except Exception as ex:
        print(f"error: {ex}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
