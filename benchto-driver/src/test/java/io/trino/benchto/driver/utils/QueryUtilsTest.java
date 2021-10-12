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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static io.trino.benchto.driver.utils.QueryUtils.compareRows;
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
}
