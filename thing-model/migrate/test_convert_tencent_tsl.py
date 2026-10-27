#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from convert_tencent_tsl import convert_document


class ConvertTencentTslTest(unittest.TestCase):

    def setUp(self) -> None:
        self.sample_path = Path(__file__).parent / "4DZDCQUA2D.json"
        with self.sample_path.open(encoding="utf-8") as stream:
            self.source = json.load(stream)

    def test_converts_sample(self) -> None:
        converted = convert_document(self.source)

        self.assertEqual("1.0", converted["version"])
        self.assertEqual("4DZDCQUA2D", converted["profile"]["ProductId"])
        self.assertEqual(3, len(converted["properties"]))
        self.assertEqual(4, len(converted["events"]))
        self.assertEqual(0, len(converted["actions"]))

        device_info = converted["properties"][0]
        specs = device_info["define"]["specs"]
        self.assertIsInstance(specs, dict)
        self.assertEqual("string", specs["company"]["type"])
        self.assertNotIn("min", specs["company"])
        self.assertNotIn("max", specs["company"])
        self.assertEqual("int", specs["battery"]["type"])
        self.assertEqual(0, specs["battery"]["min"])
        self.assertEqual(100, specs["battery"]["max"])
        self.assertEqual("%", specs["battery"]["unit"])

        real_state = converted["properties"][2]
        self.assertEqual({"0": "未工作", "1": "工作"},
                         real_state["define"]["specs"]["isWork"]["mapping"])

        alarm = converted["events"][3]
        alarm_type = next(
            param for param in alarm["params"] if param["id"] == "alarmType")
        self.assertEqual("enum_string", alarm_type["define"]["type"])
        self.assertIn("low_power", alarm_type["define"]["mapping"])

        self.assertFalse(self.contains_key(converted, "dataType"))
        self.assertFalse(self.contains_key(converted, "stringenum"))

    def test_keeps_string_ranges_when_requested(self) -> None:
        converted = convert_document(self.source, keep_string_ranges=True)

        company = converted["properties"][0]["define"]["specs"]["company"]
        self.assertEqual(0, company["min"])
        self.assertEqual(100, company["max"])

    def test_flattens_struct_properties(self) -> None:
        converted = convert_document(self.source, flatten_structs=True)

        ids = {prop["id"] for prop in converted["properties"]}
        self.assertEqual(23, len(ids))
        self.assertIn("deviceInfo_company", ids)
        self.assertIn("locationInfo_longitude", ids)
        self.assertIn("realStateInfo_machineState", ids)
        self.assertNotIn("deviceInfo", ids)
        self.assertFalse(any("." in prop_id for prop_id in ids))

    def test_converts_numeric_strings_and_string_enum(self) -> None:
        converted = convert_document({
            "version": "1.0",
            "profile": {"ProductId": "P1"},
            "properties": [{
                "id": "speed",
                "name": "Speed",
                "mode": "r",
                "define": {
                    "type": "float",
                    "min": "0",
                    "max": "2.5",
                    "start": "0.1",
                    "step": "0.01",
                    "unit": "m/s",
                },
            }],
            "events": [{
                "id": "alarm",
                "name": "Alarm",
                "type": "alert",
                "params": [{
                    "id": "kind",
                    "name": "Kind",
                    "define": {
                        "type": "stringenum",
                        "mapping": {"a": "A", "b": "B"},
                    },
                }],
            }],
            "actions": [],
        })

        speed = converted["properties"][0]["define"]
        self.assertEqual(0, speed["min"])
        self.assertEqual(2.5, speed["max"])
        self.assertEqual(0.1, speed["start"])
        self.assertEqual(0.01, speed["step"])
        self.assertEqual("enum_string",
                         converted["events"][0]["params"][0]["define"]["type"])

    def test_errors_for_non_object_source(self) -> None:
        with self.assertRaises(ValueError):
            convert_document([])

    def contains_key(self, value: object, key: str) -> bool:
        if isinstance(value, dict):
            if key in value:
                return True
            return any(self.contains_key(item, key) for item in value.values())
        if isinstance(value, list):
            return any(self.contains_key(item, key) for item in value)
        return False


if __name__ == "__main__":
    unittest.main()
