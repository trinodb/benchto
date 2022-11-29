/*
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
package io.trino.benchto.driver.utils;

import com.google.common.io.Resources;
import io.trino.benchto.driver.execution.ResultComparisonException;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static io.trino.benchto.driver.utils.QueryUtils.compareCount;
import static io.trino.benchto.driver.utils.QueryUtils.compareRows;
import static io.trino.benchto.driver.utils.QueryUtils.isSelectQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class QueryUtilsTest
{
    @Test
    public void missingResultFile()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Path path = Paths.get(Resources.getResource("comparing/test1.result").getPath() + ".non-existing");
        when(resultSet.next()).thenReturn(false);
        assertThatThrownBy(() -> compareRows(path, resultSet))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("Error opening result file");
    }

    @Test
    public void resultSetEmpty()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Path path = Paths.get(Resources.getResource("comparing/test1.result").getPath());
        when(resultSet.next()).thenReturn(false);
        assertThatThrownBy(() -> compareRows(path, resultSet))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("result file has more lines");
    }

    @Test
    public void resultSetLarger()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);

        when(metaData.getColumnCount()).thenReturn(4);

        Path path = Paths.get(Resources.getResource("comparing/test1.result").getPath());

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn(2);
        when(resultSet.getObject(3)).thenReturn(3);
        when(resultSet.getObject(4)).thenReturn(4);
        when(resultSet.getMetaData()).thenReturn(metaData);
        assertThatThrownBy(() -> compareRows(path, resultSet))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("actual result has more rows");
    }

    @Test
    public void differentValues()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);

        when(metaData.getColumnCount()).thenReturn(4);

        Path path = Paths.get(Resources.getResource("comparing/test1.result").getPath());

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn(2);
        when(resultSet.getObject(3)).thenReturn(5);
        when(resultSet.getObject(4)).thenReturn(4);
        when(resultSet.getMetaData()).thenReturn(metaData);
        assertThatThrownBy(() -> compareRows(path, resultSet))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("expected 1,2,3,4, got 1,2,5,4");
    }

    @Test
    public void differentColumnsCount()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);

        when(metaData.getColumnCount()).thenReturn(3);

        Path path = Paths.get(Resources.getResource("comparing/test1.result").getPath());

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn(2);
        when(resultSet.getObject(3)).thenReturn(3);
        when(resultSet.getMetaData()).thenReturn(metaData);
        assertThatThrownBy(() -> compareRows(path, resultSet))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("expected 1,2,3,4, got 1,2,3");
    }

    @Test
    public void successful()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);

        when(metaData.getColumnCount()).thenReturn(4);

        Path path = Paths.get(Resources.getResource("comparing/test1.result").getPath());

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn(2);
        when(resultSet.getObject(3)).thenReturn(3);
        when(resultSet.getObject(4)).thenReturn(4);
        when(resultSet.getMetaData()).thenReturn(metaData);
        compareRows(path, resultSet);
    }

    @Test
    public void successfulByteArray()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        byte[] res = "1234".getBytes(StandardCharsets.UTF_8);

        when(metaData.getColumnCount()).thenReturn(2);

        Path path = Paths.get(Resources.getResource("comparing/bytearray.result").getPath());

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(res);
        when(resultSet.getObject(2)).thenReturn(1);
        when(resultSet.getMetaData()).thenReturn(metaData);
        compareRows(path, resultSet);
    }

    @Test
    public void withNewlineAtEnd()
            throws SQLException
    {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);

        when(metaData.getColumnCount()).thenReturn(4);

        Path path = Paths.get(Resources.getResource("comparing/test2.result").getPath());

        when(resultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn(2);
        when(resultSet.getObject(3)).thenReturn(3);
        when(resultSet.getObject(4)).thenReturn(4);
        when(resultSet.getMetaData()).thenReturn(metaData);
        compareRows(path, resultSet);
    }

    @Test
    public void isSelectQueryTest()
    {
        assertThat(isSelectQuery("Select a from b;")).isEqualTo(true);
        assertThat(isSelectQuery("SELECT a from b;")).isEqualTo(true);
        assertThat(isSelectQuery("Show table a;")).isEqualTo(true);
        assertThat(isSelectQuery("With Select a;")).isEqualTo(true);
        assertThat(isSelectQuery("DELETE a from b;")).isEqualTo(false);
        assertThat(isSelectQuery("Drop table a;")).isEqualTo(false);
        assertThat(isSelectQuery("Update table a;")).isEqualTo(false);
        assertThat(isSelectQuery("INSERT into table a;")).isEqualTo(false);
    }

    @Test
    public void compareCountTest()
    {
        Path path = Paths.get(Resources.getResource("comparing/count1.result").getPath());
        compareCount(path, 20);

        path = Paths.get(Resources.getResource("comparing/count2.result").getPath());
        compareCount(path, 22);

        Path path2 = Paths.get(Resources.getResource("comparing/count3.result").getPath());
        assertThatThrownBy(() -> compareCount(path2, 12))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("Result file should have exactly one row with expected count.");

        Path path3 = Paths.get(Resources.getResource("comparing/count4.result").getPath());
        assertThatThrownBy(() -> compareCount(path3, 11))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("Result file should not have more then one row.");

        Path path4 = Paths.get(Resources.getResource("comparing/count2.result").getPath());
        assertThatThrownBy(() -> compareCount(path4, 12))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("Incorrect row count, expected 22, got 12");

        Path path5 = Paths.get(Resources.getResource("comparing").getPath());
        assertThatThrownBy(() -> compareCount(path5, 12))
                .isInstanceOf(ResultComparisonException.class)
                .hasMessageContaining("Error opening result file");
    }
}
