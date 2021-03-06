package edu.berkeley.cs186.database;

import edu.berkeley.cs186.database.Database;
import edu.berkeley.cs186.database.TestUtils;
import edu.berkeley.cs186.database.StudentTest;
import edu.berkeley.cs186.database.table.*;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.rules.TemporaryFolder;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class TestDatabase {
  public static final String TestDir = "testDatabase";
  private Database db;
  private String filename;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  @Rule
  public Timeout globalTimeout = Timeout.seconds(10); // 10 seconds max per method tested

  @Before
  public void beforeEach() throws Exception {
    File testDir = tempFolder.newFolder(TestDir);
    this.filename = testDir.getAbsolutePath();
    this.db = new Database(filename);
    this.db.deleteAllTables();
  }

  @After
  public void afterEach() {
    this.db.deleteAllTables();
    this.db.close();
  }

  @Test
  public void testTableCreate() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();
    
    db.createTable(s, "testTable1"); 
  }

  @Test
  public void testTransactionBegin() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();
    Record input = TestUtils.createRecordWithAllTypes();

    String tableName = "testTable1"; 
    db.createTable(s, tableName);
    
    Database.Transaction t1 = db.beginTransaction();
    RecordID rid = t1.addRecord(tableName, input.getValues());
    Record rec = t1.getRecord(tableName, rid);
    assertEquals(input, rec);
    t1.end();
  }
  
  @Test
  public void testTransactionTempTable() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();
    Record input = TestUtils.createRecordWithAllTypes();

    String tableName = "testTable1"; 
    db.createTable(s, tableName);
    
    Database.Transaction t1 = db.beginTransaction();
    RecordID rid = t1.addRecord(tableName, input.getValues());
    Record rec = t1.getRecord(tableName, rid);
    assertEquals(input, rec);
    
    t1.createTempTable(s, "temp1");
    rid = t1.addRecord("temp1", input.getValues());
    rec = t1.getRecord("temp1", rid);
    assertEquals(input, rec);
    t1.end();
  }
  
  @Test(expected = DatabaseException.class)  
  public void testTransactionTempTable2() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();
    Record input = TestUtils.createRecordWithAllTypes();

    String tableName = "testTable1"; 
    db.createTable(s, tableName);
    
    Database.Transaction t1 = db.beginTransaction();
    RecordID rid = t1.addRecord(tableName, input.getValues());
    Record rec = t1.getRecord(tableName, rid);
    assertEquals(input, rec);
    
    t1.createTempTable(s, "temp1");
    rid = t1.addRecord("temp1", input.getValues());
    rec = t1.getRecord("temp1", rid);
    assertEquals(input, rec);
    t1.end();
    Database.Transaction t2 = db.beginTransaction();
    rid = t2.addRecord("temp1", input.getValues());
    rec = t1.getRecord("temp1", rid);
    assertEquals(input, rec);
    t2.end();
  }
  @Test
  public void testTransactionIndexInit() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();
    Record input = TestUtils.createRecordWithAllTypes();

    String tableName = "testTable1";
    List<String> indexNames = new ArrayList<String>();
    indexNames.add("int");
    indexNames.add("string");
    indexNames.add("float");
    db.createTableWithIndices(s, tableName, indexNames);
    
    Database.Transaction t1 = db.beginTransaction();
    RecordID rid = t1.addRecord(tableName, input.getValues());
    Record rec = t1.getRecord(tableName, rid);
    assertEquals(input, rec);
    t1.end();
  }
  
  @Test
  public void testTransactionIndexInsertBulk() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();

    String tableName = "testTable1";
    List<String> indexNames = new ArrayList<String>();
    indexNames.add("int");
    indexNames.add("string");
    indexNames.add("float");
    db.createTableWithIndices(s, tableName, indexNames);
    
    Database.Transaction t1 = db.beginTransaction();
    
    assertTrue(t1.indexExists(tableName, "int"));
    assertTrue(t1.indexExists(tableName, "string"));
    assertTrue(t1.indexExists(tableName, "float"));
    
    for (int i = 500; i >= 0; i--) {
      Record input = TestUtils.createRecordWithAllTypesWithValue(i);
      RecordID rid = t1.addRecord(tableName, input.getValues());
    }
    for (String col : indexNames) { 
      Iterator<Record> recIter = t1.sortedScan(tableName, col);
      for (int i = 0; i <= 500; i++) {
        assertTrue(recIter.hasNext());
        Record rec = recIter.next();
        Record input = TestUtils.createRecordWithAllTypesWithValue(i);
        assertEquals(input, rec);
      }
      assertFalse(recIter.hasNext());
    }
    t1.end();
  }
  
  @Test
  public void testDatabaseDurablity() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();
    Record input = TestUtils.createRecordWithAllTypes();

    String tableName = "testTable1"; 
    db.createTable(s, tableName);
    
    Database.Transaction t1 = db.beginTransaction();
    RecordID rid = t1.addRecord(tableName, input.getValues());
    Record rec = t1.getRecord(tableName, rid);
    assertEquals(input, rec);
    
    t1.end();
    db.close();
    
    db = new Database(this.filename);
    t1 = db.beginTransaction();
    rec = t1.getRecord(tableName, rid);
    assertEquals(input, rec);
    t1.end();
  }
  
  @Test
  public void testDatabaseDurablityBulkWithIndices() throws DatabaseException {
    Schema s = TestUtils.createSchemaWithAllTypes();

    String tableName = "testTable1";
    List<String> indexNames = new ArrayList<String>();
    indexNames.add("int");
    indexNames.add("string");
    indexNames.add("float");
    db.createTableWithIndices(s, tableName, indexNames);
    
    Database.Transaction t1 = db.beginTransaction();
    
    assertTrue(t1.indexExists(tableName, "int"));
    assertTrue(t1.indexExists(tableName, "string"));
    assertTrue(t1.indexExists(tableName, "float"));
    
    for (int i = 500; i >= 0; i--) {
      Record input = TestUtils.createRecordWithAllTypesWithValue(i);
      RecordID rid = t1.addRecord(tableName, input.getValues());
    }
    
    t1.end();
    db.close();
    
    db = new Database(this.filename);
    t1 = db.beginTransaction();
    
    assertTrue(t1.indexExists(tableName, "int"));
    assertTrue(t1.indexExists(tableName, "string"));
    assertTrue(t1.indexExists(tableName, "float"));
    
    for (String col : indexNames) { 
      Iterator<Record> recIter = t1.sortedScan(tableName, col);
      for (int i = 0; i <= 500; i++) {
        assertTrue(recIter.hasNext());
        Record rec = recIter.next();
        Record input = TestUtils.createRecordWithAllTypesWithValue(i);
        assertEquals(input, rec);
      }
      assertFalse(recIter.hasNext());
    }
    t1.end();
  }
}
