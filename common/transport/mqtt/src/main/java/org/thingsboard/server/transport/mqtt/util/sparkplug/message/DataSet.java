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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.mqtt.util.sparkplug.json.DataSetDeserializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A data set that represents a table of data.
 */
@Slf4j
@JsonDeserialize(using = DataSetDeserializer.class)
public class DataSet {

	/**
	 * The number of columns
	 */
	@JsonProperty("numberOfColumns")
	private long numOfColumns;
	
	/**
	 * A list containing the names of each column
	 */
	@JsonProperty("columnNames")
	private List<String> columnNames;
	
	/**
	 * A list containing the data types of each column
	 */
	@JsonProperty("types")
	private List<DataSetDataType> types;
	
	/**
	 * A list containing the rows in the data set
	 */
	private List<Row> rows;
	
	public DataSet(long numOfColumns, List<String> columnNames, List<DataSetDataType> types, List<Row> rows) {
		this.numOfColumns = numOfColumns;
		this.columnNames = columnNames;
		this.types = types;
		this.rows = rows;
	}

	public long getNumOfColumns() {
		return numOfColumns;
	}

	public void setNumOfColumns(long numOfColumns) {
		this.numOfColumns = numOfColumns;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(List<String> columnNames) {
		this.columnNames = columnNames;
	}

	public void addColumnName(String columnName) {
		this.columnNames.add(columnName);
	}

	public List<Row> getRows() {
		return rows;
	}
	
	@JsonGetter("rows")
	public List<List<Object>> getRowsAsLists() {
		List<List<Object>> list = new ArrayList<List<Object>>(getRows().size());
		for (Row row : getRows()) {
			list.add(Row.toValues(row));
		}
		return list;
	}

	public void addRow(Row row) {
		this.rows.add(row);
	}
	
	public void addRow(int index, Row row) {
		this.rows.add(index, row);
	}
	
	public Row removeRow(int index) {
		return rows.remove(index);
	}
	
	public boolean removeRow(Row row) {
		return rows.remove(row);
	}
	
	public void setRows(List<Row> rows) {
		this.rows = rows;
	}

	public List<DataSetDataType> getTypes() {
		return types;
	}

	public void setTypes(List<DataSetDataType> types) {
		this.types = types;
	}
	
	public void addType(DataSetDataType type) {
		this.types.add(type);
	}
	
	public void addType(int index, DataSetDataType type) {
		this.types.add(index, type);
	}
	
	@Override
	public String toString() {
		return "DataSet [numOfColumns=" + numOfColumns + ", columnNames=" + columnNames + ", types=" + types + ", rows="
				+ rows + "]";
	}

	/**
	 * A builder for creating a {@link DataSet} instance.
	 */
	public static class DataSetBuilder {

		private long numOfColumns;
		private List<String> columnNames;
		private List<DataSetDataType> types;
		private List<Row> rows;

		public DataSetBuilder(long numOfColumns) {
			this.numOfColumns = numOfColumns;
			this.columnNames = new ArrayList<String>();
			this.types = new ArrayList<DataSetDataType>();
			this.rows =  new ArrayList<Row>();
		}
		
		public DataSetBuilder(DataSet dataSet) {
			this.numOfColumns = dataSet.getNumOfColumns();
			this.columnNames = new ArrayList<String>(dataSet.getColumnNames());
			this.types = new ArrayList<DataSetDataType>(dataSet.getTypes());
			this.rows =  new ArrayList<Row>(dataSet.getRows().size());
			for (Row row : dataSet.getRows()) {
				rows.add(new Row.RowBuilder(row).createRow());
			}
		}

		public DataSetBuilder addColumnNames(Collection<String> columnNames) {
			this.columnNames.addAll(columnNames);
			return this;
		}

		public DataSetBuilder addColumnName(String columnName) {
			this.columnNames.add(columnName);
			return this;
		}

		public DataSetBuilder addType(DataSetDataType type) {
			this.types.add(type);
			return this;
		}

		public DataSetBuilder addTypes(Collection<DataSetDataType> types) {
			this.types.addAll(types);
			return this;
		}

		public DataSetBuilder addRow(Row row) {
			this.rows.add(row);
			return this;
		}

		public DataSetBuilder addRows(Collection<Row> rows) {
			this.rows.addAll(rows);
			return this;
		}
		
		public DataSet createDataSet() throws Exception {
			log.trace("Number of columns: " + numOfColumns);
			for (String columnName : columnNames) {
				log.trace("\tcolumnName: " + columnName);
			}
			for (DataSetDataType type : types) {
				log.trace("\ttypes: " + type);
			}
			for (Row row : rows) {
				log.trace("\t\trow: " + row);
			}

			validate();
			return new DataSet(numOfColumns, columnNames, types, rows);
		}
		
		public void validate() throws Exception {
			if (columnNames.size() != numOfColumns) {
				throw new Exception("Invalid number of columns in data set column names: " +
						columnNames.size() + " vs expected " + numOfColumns);
			}
			if (types.size() != numOfColumns) {
				throw new Exception("Invalid number of columns in data set types: " +
						types.size() + " vs expected: " + numOfColumns);
			}
			for (int i = 0; i < types.size(); i++) {
				for (Row row : rows) {
					List<SparkplugValue<?>> values = row.getValues();
					if (values.size() != numOfColumns) {
						throw new Exception("Invalid number of columns in data set row: " +
								values.size() + " vs expected: " + numOfColumns);
					}
					types.get(i).checkType(row.getValues().get(i).getValue());
				}
			}
		}
	}
}
