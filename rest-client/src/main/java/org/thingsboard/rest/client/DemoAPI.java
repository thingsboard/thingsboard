package org.thingsboard.rest.client;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DemoAPI {

	public static void main(String[] args) {
		String url = "http://thingsinfo.cn:1088/";

		// Default Tenant Administrator credentials
		String username = "demo@thingsinfo.cn";
		String password = "thingsinfo";

		RestClient client = new RestClient(url);
		client.login(username, password);


		PageLink pageLink = new PageLink(100, 0);
		/*List<Device> deviceList = client.getTenantDevices(null, pageLink).getData();
		System.out.println(deviceList);*/
		DeviceId id = new DeviceId(UUID.fromString("8bb40c70-c389-11ea-a561-73726fbc5ab7"));
		List<String> keys = new ArrayList<>();
		keys.add("power");
		Long st = new Long("1595217324000");
		Long en = new Long("1595261048000");
		Long inter = Long.getLong("0");
		SortOrder sortOrder = new SortOrder("ASC");

		TimePageLink timePageLink = new TimePageLink(100,0,100,null, sortOrder,st,en);

		List<TsKvEntry> list = client.getTimeseries(id,keys,inter, Aggregation.NONE,timePageLink);

		System.out.println(list);

	}

}
