#!/usr/bin/env python3
from __future__ import annotations

import random
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from simulate_telemetry import TelemetryGenerator


class EventGenerationTest(unittest.TestCase):

    def setUp(self) -> None:
        self.properties = [
            {
                "id": "speed",
                "define": {"type": "float", "min": 0, "max": 100},
            }
        ]
        self.events = [
            {
                "id": "refuelRecord",
                "type": "info",
                "params": [
                    {
                        "id": "volume",
                        "define": {"type": "float", "min": 0, "max": 100},
                    },
                    {
                        "id": "tankId",
                        "define": {"type": "int", "min": 0, "max": 10},
                    },
                ],
            },
            {
                "id": "alarm",
                "type": "alert",
                "params": [
                    {
                        "id": "address",
                        "define": {"type": "string"},
                    }
                ],
            },
        ]
        self.generator = TelemetryGenerator(
            self.properties, self.events, random.Random(1))

    def test_event_batch_uses_single_event_type(self) -> None:
        payload = self.generator.event_batch()

        self.assertIsInstance(payload, dict)
        batch = payload["events"]
        self.assertEqual(4, len(batch))
        event_ids = {item["event_id"] for item in batch}
        self.assertEqual(1, len(event_ids))
        self.assertIn(event_ids.pop(), {"refuelRecord", "alarm"})
        event_types = {item["event_type"] for item in batch}
        self.assertEqual(1, len(event_types))
        self.assertEqual(4, len({item["event_uid"] for item in batch}))
        base_ts = batch[0]["event_ts"]
        self.assertEqual(
            [base_ts + index for index in range(4)],
            [item["event_ts"] for item in batch])

    def test_payload_event_array_true_uses_batch(self) -> None:
        payload = self.generator.payload("event", event_array=True)

        self.assertIsInstance(payload, dict)
        self.assertEqual(4, len(payload["events"]))

    def test_events_per_packet_is_configurable(self) -> None:
        payload = self.generator.payload(
            "event", event_array=True, events_per_packet=2)

        self.assertEqual(2, len(payload["events"]))
        self.assertEqual(1, len({item["event_id"] for item in payload["events"]}))

    def test_payload_event_array_false_uses_single_event(self) -> None:
        payload = self.generator.payload("event", event_array=False)

        self.assertIsInstance(payload, dict)
        self.assertNotIn("events", payload)
        self.assertIn(payload["event_id"], {"refuelRecord", "alarm"})
        self.assertIn("event_ts", payload)

    def test_payload_telemetry_is_object(self) -> None:
        payload = self.generator.payload("telemetry", event_array=True)

        self.assertIsInstance(payload, dict)
        self.assertIn("speed", payload)


if __name__ == "__main__":
    unittest.main()
