import asyncio
import logging
from asyncua import Server, ua


NUM_AIRCONS = 10


async def set_mode(parent, mode):
    print(f"SetMode called with mode={mode}")
    return [ua.Variant(True, ua.VariantType.Boolean)]


async def create_aircon(idx, parent, number: int):

    aircon = await parent.add_object(idx, f"AirConditioner_{number}")

    vars_ = {}

    vars_["Humidity"] = await aircon.add_variable(
        idx, "Humidity", ua.Variant(50.0, ua.VariantType.Double)
    )
    vars_["HumiditySetpoint"] = await aircon.add_variable(
        idx, "HumiditySetpoint", ua.Variant(55.0, ua.VariantType.Double)
    )
    vars_["Temperature"] = await aircon.add_variable(
        idx, "Temperature", ua.Variant(22.0, ua.VariantType.Double)
    )
    vars_["TemperatureSetpoint"] = await aircon.add_variable(
        idx, "TemperatureSetpoint", ua.Variant(24.0, ua.VariantType.Double)
    )
    vars_["PowerConsumption"] = await aircon.add_variable(
        idx, "PowerConsumption", ua.Variant(0.0, ua.VariantType.Double)
    )
    vars_["Enabled"] = await aircon.add_variable(
        idx, "Enabled", ua.Variant(True, ua.VariantType.Boolean)
    )
    vars_["ErrorCode"] = await aircon.add_variable(
        idx, "ErrorCode", ua.Variant(0, ua.VariantType.Int32)
    )
    vars_["RuntimeHours"] = await aircon.add_variable(
        idx, "RuntimeHours", ua.Variant(0, ua.VariantType.Int64)
    )
    vars_["Mode"] = await aircon.add_variable(
        idx, "Mode", ua.Variant("OFF", ua.VariantType.String)
    )

    for v in vars_.values():
        await v.set_writable()

    await aircon.add_method(
        idx,
        "SetMode",
        set_mode,
        [ua.VariantType.String],
        [ua.VariantType.Boolean],
    )

    return vars_


async def main():
    logging.basicConfig(level=logging.INFO)
    log = logging.getLogger("aircon-opcua-server")

    server = Server()
    await server.init()
    server.set_endpoint("opc.tcp://0.0.0.0:4840/aircon/")

    uri = "http://tb.opcuatest/BuildingAutomation"
    idx = await server.register_namespace(uri)

    objects = server.nodes.objects
    building = await objects.add_object(idx, "BuildingAutomation")

    aircons = []
    for i in range(1, NUM_AIRCONS + 1):
        vars_ = await create_aircon(idx, building, i)
        aircons.append(vars_)

    log.info("OPC UA AirConditioner server started at opc.tcp://0.0.0.0:4840/aircon/")

    async with server:
        while True:
            for vars_ in aircons:
                h = await vars_["Humidity"].get_value()
                await vars_["Humidity"].write_value(float(h + 0.1) % 100)
            await asyncio.sleep(1)


if __name__ == "__main__":
    asyncio.run(main())
