/*
 * Copyright (C) 2018-2020. Huawei Technologies Co., Ltd. All rights reserved.
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

package io.prestosql.catalog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Key;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;

import static io.prestosql.client.PrestoHeaders.PRESTO_USER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestCatalogResource
        extends TestDynamicCatalogRunner
{
    public TestCatalogResource()
            throws Exception
    {
    }

    @Test
    public void testCheckFileName()
    {
        CatalogResource resource = server.getInstance(Key.get(CatalogResource.class));
        assertTrue(resource.checkFileName("keystore.jks"));
        assertFalse(resource.checkFileName("/dir/keystore.jks"));
        assertFalse(resource.checkFileName("keystore.exe"));
    }

    @Test
    public void testAddCatalog()
            throws Exception
    {
        String catalogName = "tpch0";
        assertTrue(executeAddCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "The catalog .tpch1. already exists")
    public void testAddCatalogWithExistException()
            throws Exception
    {
        String catalogName = "tpch1";
        assertTrue(executeAddCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
        executeAddCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of());
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Try to load catalog failed, check your configuration. cause by No factory for connector \\[tpch1\\].  Available factories: \\[system, tpch\\]")
    public void testAddCatalogWithInvalidPropertiesFile()
            throws Exception
    {
        String catalogName = "tpch3";
        assertTrue(executeAddCatalogCall(catalogName, "tpch1", ImmutableMap.of(), ImmutableList.of(), ImmutableList.of()));
    }

    @Test
    public void testDropCatalog()
            throws Exception
    {
        String catalogName = "tpch4";
        assertTrue(executeAddCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
        assertTrue(executeDeleteCatalogCall(catalogName));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "The catalog .tpch5. does not exist")
    public void testDropCatalogWithNotExistException()
            throws Exception
    {
        executeDeleteCatalogCall("tpch5");
    }

    @Test
    public void testShowCatalogs()
            throws Exception
    {
        String catalogName = "tpch6";
        assertTrue(executeAddCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
        assertTrue(executeShowCatalogCall().contains(catalogName));
    }

    @Test
    public void testUpdateCatalog()
            throws Exception
    {
        String catalogName = "tpch7";
        assertTrue(executeAddCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
        assertTrue(executeUpdateCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "The catalog .tpch8. does not exist")
    public void testUpdateCatalogWithExistException()
            throws Exception
    {
        String catalogName = "tpch8";
        assertTrue(executeUpdateCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Try to load catalog failed, check your configuration. cause by No factory for connector \\[tpch1\\].  Available factories: \\[system, tpch\\]")
    public void testUpdateCatalogWithInvalidPropertiesFile()
            throws Exception
    {
        String catalogName = "tpch9";
        assertTrue(executeAddCatalogCall(catalogName, "tpch", tpchProperties, ImmutableList.of(), ImmutableList.of()));
        assertTrue(executeUpdateCatalogCall(catalogName, "tpch1", ImmutableMap.of(), ImmutableList.of(), ImmutableList.of()));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Try to load catalog failed, check your configuration. cause by No factory for connector \\[hive-hadoop2\\].  Available factories: \\[system, tpch\\]")
    public void testAddHiveCatalogFail()
            throws Exception
    {
        String catalogName = "hive0";

        List<URL> catalogFiles = new ImmutableList.Builder<URL>()
                .add(Resources.getResource("dynamiccatalog/catalog/hive/core-site.xml"))
                .add(Resources.getResource("dynamiccatalog/catalog/hive/hdfs-site.xml"))
                .add(Resources.getResource("dynamiccatalog/catalog/hive/user.keytab"))
                .build();
        List<URL> globalFiles = new ImmutableList.Builder<URL>()
                .add(Resources.getResource("dynamiccatalog/global/krb5.conf"))
                .build();

        executeAddCatalogCall(catalogName, "hive-hadoop2", hiveProperties, catalogFiles, globalFiles);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Catalog information is missing")
    // invalid catalog information
    public void testAddHiveCatalogCatalogInformationInvalid001()
            throws Exception
    {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(server.getBaseUrl() + "/v1/catalog/");

        MultipartEntity multipartEntity = new MultipartEntity();
        StringBody stringBody = new StringBody("catalogInformation123");
        multipartEntity.addPart("catalogInformation123", stringBody);

        httpPost.setEntity(multipartEntity);
        httpPost.setHeader(PRESTO_USER, "admin");

        HttpResponse response = httpclient.execute(httpPost);
        if (response != null) {
            if (response.getStatusLine().getStatusCode() != CREATED.getStatusCode()) {
                throw new RuntimeException(EntityUtils.toString(response.getEntity(), UTF_8));
            }
        }
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Invalid JSON string of catalog information")
    // invalid catalog name
    public void testAddHiveCatalogCatalogInformationInvalid002()
            throws Exception
    {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(server.getBaseUrl() + "/v1/catalog/");

        MultipartEntity multipartEntity = new MultipartEntity();
        StringBody stringBody = new StringBody("\"catalogName123\" : \"hive\",\n" +
                "  \"connectorName\" : \"hive-hadoop2\"");
        multipartEntity.addPart("catalogInformation", stringBody);

        httpPost.setEntity(multipartEntity);
        httpPost.setHeader(PRESTO_USER, "admin");

        HttpResponse response = httpclient.execute(httpPost);
        if (response != null) {
            if (response.getStatusLine().getStatusCode() != CREATED.getStatusCode()) {
                throw new RuntimeException(EntityUtils.toString(response.getEntity(), UTF_8));
            }
        }
    }
}
