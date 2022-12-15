/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.mqtt.util.sparkplug.message;

import org.thingsboard.server.transport.mqtt.util.sparkplug.json.FileSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Arrays;

@JsonIgnoreProperties(value = { "fileName" })
@JsonSerialize(using = FileSerializer.class)
public class File {
	
	private String fileName;
	private byte[] bytes;
	
	public File() {
		super();
	}
	
	public File(String fileName, byte[] bytes) {
		super();
		this.fileName = fileName == null 
				? null 
				: fileName.replace("/", System.getProperty("file.separator"))
						.replace("\\", System.getProperty("file.separator"));
		this.bytes = Arrays.copyOf(bytes, bytes.length);
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
	
	@Override
	public String toString() {
		return "File [fileName=" + fileName + ", bytes length=" + bytes.length + "]";
	}
}
