package au.org.intersect.faims.android.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jsqlite.Callback;
import jsqlite.Stmt;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.form.ArchEntity;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.Relationship;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;
import au.org.intersect.faims.android.util.DateUtil;

import com.google.inject.Singleton;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

@Singleton
public class DatabaseManager {

	private String dbname;
	private String userId;
	
	private jsqlite.Database db;

	public void init(String filename) {
		this.dbname = filename;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getUserId() {
		return this.userId;
	}
	
	public void interrupt() {
		try {
			if (db != null) {
				db.close();
				db = null;
			}
		} catch (Exception e) {
			FLog.e("error closing database", e);
		}
	}

	public String saveArchEnt(String entity_id, String entity_type,
			String geo_data, List<EntityAttribute> attributes) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("entity_id:" + entity_id);
			FLog.d("entity_type:" + entity_type);
			FLog.d("geo_data:" + geo_data);
			
			for (EntityAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
			
			Stmt st = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!validArchEnt(db, entity_id, entity_type, geo_data, attributes)) {
					FLog.d("arch entity not valid");
					return null;
				}
				
				String uuid;
				
				if (entity_id == null) {
					// create new entity
					uuid = generateUUID();
				} else {
					// update entity
					uuid = entity_id;
				}
				
				String currentTimestamp = DateUtil.getCurrentTimestampGMT();
				
				String query = "INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, AEntTimestamp) " +
									"SELECT cast(? as integer), ?, aenttypeid, GeomFromText(?, 4326), ? " +
									"FROM aenttype " + 
									"WHERE aenttypename = ? COLLATE NOCASE;";
				st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, userId);
				st.bind(3, geo_data);
				st.bind(4, currentTimestamp);
				st.bind(5, entity_type);
				st.step();
				st.close();
				st = null;
				
				// save entity attributes
				for (EntityAttribute attribute : attributes) {
					query = "INSERT INTO AEntValue (uuid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp) " +
								   "SELECT cast(? as integer), ?, attributeID, ?, ?, ?, ? " +
								   "FROM AttributeKey " + 
								   "WHERE attributeName = ? COLLATE NOCASE;";
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, attribute.getVocab());
					st.bind(3, attribute.getMeasure());
					st.bind(4, attribute.getText());
					st.bind(5, attribute.getCertainty());
					st.bind(6, currentTimestamp);
					st.bind(7, attribute.getName());
					st.step();
					st.close();
					st = null;
				}
				
				return uuid;
				
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public String saveRel(String rel_id, String rel_type,
			String geo_data, List<RelationshipAttribute> attributes) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("rel_id:" + rel_id);
			FLog.d("rel_type:" + rel_type);
			FLog.d("geo_data:" + geo_data);
			
			for (RelationshipAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
			
			Stmt st = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!validRel(db, rel_id, rel_type, geo_data, attributes)) {
					FLog.d("relationship not valid");
					return null;
				}
				
				String uuid;
				
				if (rel_id == null) {
					// create new relationship
					uuid = generateUUID();
					
				} else {
					
					uuid = rel_id;
				}
				
				String currentTimestamp = DateUtil.getCurrentTimestampGMT();
				
				String query = "INSERT INTO Relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumn, RelnTimestamp) " +
									"SELECT cast(? as integer), ?, relntypeid, GeomFromText(?, 4326), ? " +
									"FROM relntype " +
									"WHERE relntypename = ? COLLATE NOCASE;";
				st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, userId);
				st.bind(3, geo_data);
				st.bind(4, currentTimestamp);
				st.bind(5, rel_type);
				st.step();
				st.close();
				st = null;
				
				// save relationship attributes
				for (RelationshipAttribute attribute : attributes) {
					query = "INSERT INTO RelnValue (RelationshipID, VocabID, AttributeID, FreeText, Certainty, RelnValueTimestamp) " +
								   "SELECT cast(? as integer), ?, attributeId, ?, ?, ? " +
								   "FROM AttributeKey " + 
								   "WHERE attributeName = ? COLLATE NOCASE;";
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, attribute.getVocab());
					st.bind(3, attribute.getText());
					st.bind(4, attribute.getCertainty());
					st.bind(5, currentTimestamp);
					st.bind(6, attribute.getName());
					st.step();
					st.close();
					st = null;
				}
				
				return uuid;
				
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
			
		}
	}
	
	private boolean validArchEnt(jsqlite.Database db, String entity_id, String entity_type, String geo_data, List<EntityAttribute> attributes) throws Exception {
		Stmt st = null;
		try {
			if (entity_id == null && !hasEntityType(db, entity_type)) {
				return false;
			} else if (entity_id != null && !hasEntity(db, entity_id)) {
				return false;
			}
			
			// check if attributes exist
			for (EntityAttribute attribute : attributes) {
				String query = "SELECT count(AEntTypeName) " + 
							   "FROM IdealAEnt left outer join AEntType using (AEntTypeId) left outer join AttributeKey using (AttributeId) " + 
							   "WHERE AEntTypeName = ? COLLATE NOCASE and AttributeName = ? COLLATE NOCASE;";
				
				st = db.prepare(query);
				st.bind(1, entity_type);
				st.bind(2, attribute.getName());
				st.step();
				if (st.column_int(0) == 0) {
					return false;
				}
				st.close();
				st = null;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean validRel(jsqlite.Database db, String rel_id, String rel_type, String geo_data, List<RelationshipAttribute> attributes) throws Exception {
		Stmt st = null;
		try {
			if (rel_id == null && !hasRelationshipType(db, rel_type)) {
				return false;
			} else if (rel_id != null && !hasRelationship(db, rel_id)) {
				return false;
			}
			
			// check if attributes exist
			for (RelationshipAttribute attribute : attributes) {
				String query = "SELECT count(RelnTypeName) " + 
						   	   "FROM IdealReln left outer join RelnType using (RelnTypeID) left outer join AttributeKey using (AttributeId) " + 
						       "WHERE RelnTypeName = ? COLLATE NOCASE and AttributeName = ? COLLATE NOCASE;";
				st = db.prepare(query);
				st.bind(1, rel_type);
				st.bind(2, attribute.getName());
				st.step();
				if (st.column_int(0) == 0) {
					return false;
				}
				st.close();
				st = null;
			}
		} finally {
			if (st != null) st.close();
		}
		
		return true;
	}
	
	public boolean addReln(String entity_id, String rel_id, String verb) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("entity_id:" + entity_id);
			FLog.d("rel_id:" + rel_id);
			FLog.d("verb:" + verb);
			
			Stmt st = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!hasEntity(db, entity_id) || !hasRelationship(db, rel_id)) {
					FLog.d("cannot add entity to relationship");
					return false;
				}
				
				String currentTimestamp = DateUtil.getCurrentTimestampGMT();
				
				// create new entity relationship
				String query = "INSERT INTO AEntReln (UUID, RelationshipID, ParticipatesVerb, AEntRelnTimestamp) " +
							   "VALUES (?, ?, ?, ?);";
				st = db.prepare(query);
				st.bind(1, entity_id);
				st.bind(2, rel_id);
				st.bind(3, verb);
				st.bind(4, currentTimestamp);
				st.step();
				st.close();
				st = null;
				
				return true;
				
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Object fetchArchEnt(String id) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				if (!hasEntity(db, id)) {
					return null;
				}
	
				String query = "SELECT uuid, attributename, vocabid, measure, freetext, certainty, AEntTypeID, aenttimestamp, valuetimestamp FROM " +
								    "(SELECT uuid, attributeid, vocabid, measure, freetext, certainty, valuetimestamp FROM aentvalue WHERE uuid || valuetimestamp || attributeid in " +
								        "(SELECT uuid || max(valuetimestamp) || attributeid FROM aentvalue WHERE uuid = ? GROUP BY uuid, attributeid having deleted is null) ) " +
								"JOIN attributekey USING (attributeid) " +
								"JOIN ArchEntity USING (uuid) " +
								"where uuid || aenttimestamp in ( select uuid || max(aenttimestamp) from archentity group by uuid having deleted is null);";
				stmt = db.prepare(query);
				stmt.bind(1, id);
				Collection<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
				String type = null;
				while(stmt.step()){
					type = stmt.column_string(6);
					EntityAttribute archAttribute = new EntityAttribute();
					archAttribute.setName(stmt.column_string(1));
					archAttribute.setVocab(Integer.toString(stmt.column_int(2)));
					archAttribute.setMeasure(Double.toString(stmt.column_double(3)));
					archAttribute.setText(stmt.column_string(4));
					archAttribute.setCertainty(Double.toString(stmt.column_double(5)));
					attributes.add(archAttribute);
				}
				stmt.close();
				stmt = null;
				
				// get vector geometry
				stmt = db.prepare("SELECT uuid, HEX(AsBinary(GeoSpatialColumn)) from ArchEntity where uuid || aenttimestamp IN ( SELECT uuid || max(aenttimestamp) FROM archentity WHERE uuid = ?);");
				stmt.bind(1, id);
				List<Geometry> geomList = new ArrayList<Geometry>();
				if(stmt.step()){
					Geometry[] g1 = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(1))), null);
					if (g1 != null) {
			            for (int i = 0; i < g1.length; i++) {
			                geomList.add(GeometryUtil.fromGeometry(g1[i]));
			            }
					}
				}
				stmt.close();
				stmt = null;
	
				ArchEntity archEntity = new ArchEntity(id, type, attributes, geomList);
				
				return archEntity;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public Object fetchRel(String id) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				 db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				if (!hasRelationship(db, id)) {
					return null;
				}
				
				String query = "SELECT relationshipid, attributename, vocabid, freetext, certainty, relntypeid FROM " +
								    "(SELECT relationshipid, attributeid, vocabid, freetext, certainty FROM relnvalue WHERE relationshipid || relnvaluetimestamp || attributeid in " +
								        "(SELECT relationshipid || max(relnvaluetimestamp) || attributeid FROM relnvalue WHERE relationshipid = ? GROUP BY relationshipid, attributeid having deleted is null)) " +
								"JOIN attributekey USING (attributeid) " +
								"JOIN Relationship USING (relationshipid) " +
								"where relationshipid || relntimestamp in (select relationshipid || max (relntimestamp) from relationship group by relationshipid having deleted is null )";
				stmt = db.prepare(query);
				stmt.bind(1, id);
				Collection<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
				String type = null;
				while(stmt.step()){
					type = stmt.column_string(4);
					RelationshipAttribute relAttribute = new RelationshipAttribute();
					relAttribute.setName(stmt.column_string(1));
					relAttribute.setVocab(Integer.toString(stmt.column_int(2)));
					relAttribute.setText(stmt.column_string(3));
					relAttribute.setCertainty(stmt.column_string(4));
					attributes.add(relAttribute);
				}
				stmt.close();
				stmt = null;
				
				// get vector geometry
				stmt = db.prepare("SELECT relationshipid, HEX(AsBinary(GeoSpatialColumn)) from relationship where relationshipid || relntimestamp IN ( SELECT relationshipid || max(relntimestamp) FROM relationship WHERE relationshipid = ?);");
				stmt.bind(1, id);
				List<Geometry> geomList = new ArrayList<Geometry>();
				if(stmt.step()){
					Geometry[] g1 = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(1))), null);
					if (g1 != null) {
			            for (int i = 0; i < g1.length; i++) {
			                geomList.add(GeometryUtil.fromGeometry(g1[i]));
			            }
					}
				}
				stmt.close();
				stmt = null;
				
				Relationship relationship = new Relationship(id, type, attributes, geomList);
	
				return relationship;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Object fetchOne(String query) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				stmt = db.prepare(query);
				Collection<String> results = new ArrayList<String>();
				if(stmt.step()){
					for(int i = 0; i < stmt.column_count(); i++){
						results.add(stmt.column_string(i));
					}
				}
				stmt.close();
				stmt = null;
				
				return results;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Collection<List<String>> fetchAll(String query) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				stmt = db.prepare(query);
				Collection<List<String>> results = new ArrayList<List<String>>();
				while(stmt.step()){
					List<String> result = new ArrayList<String>();
					for(int i = 0; i < stmt.column_count(); i++){
						result.add(stmt.column_string(i));
					}
					results.add(result);
				}
				stmt.close();
				stmt = null;
	
				return results;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	private boolean hasEntityType(jsqlite.Database db, String entity_type) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(AEntTypeID) from AEntType where AEntTypeName = ? COLLATE NOCASE;");
			st.bind(1, entity_type);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("entity type does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean hasEntity(jsqlite.Database db, String entity_id) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(UUID) from ArchEntity where UUID = ?;");
			st.bind(1, entity_id);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("entity id " + entity_id + " does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean hasRelationshipType(jsqlite.Database db, String rel_type) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(RelnTypeID) from RelnType where RelnTypeName = ? COLLATE NOCASE;");
			st.bind(1, rel_type);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("rel type does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean hasRelationship(jsqlite.Database db, String rel_id) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(RelationshipID) from Relationship where RelationshipID = ?;");
			st.bind(1, rel_id);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("rel id " + rel_id + " does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private Callback createCallback() {
		return new Callback() {
			@Override
			public void columns(String[] coldata) {
				FLog.d("Columns: " + Arrays.toString(coldata));
			}

			@Override
			public void types(String[] types) {
				FLog.d("Types: " + Arrays.toString(types));
			}

			@Override
			public boolean newrow(String[] rowdata) {
				FLog.d("Row: " + Arrays.toString(rowdata));

				return false;
			}
		};
	}
	
	private String generateUUID() {
		String s = userId;
		while (s.length() < 5) {
			s = "0" + s;
		}
		return "1"+ s + String.valueOf(System.currentTimeMillis());
	}

	public void dumpDatabaseTo(File file) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("dumping database to " + file.getAbsolutePath());
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
	
				String query = 
							"attach database '" + file.getAbsolutePath() + "' as export;" +
							"create table export.archentity as select * from archentity;" +
							"create table export.aentvalue as select * from aentvalue;" +
							"create table export.aentreln as select * from aentreln;" + 
							"create table export.relationship as select * from relationship;" +
							"create table export.relnvalue as select * from relnvalue;" +
							"detach database export;";
				db.exec(query, createCallback());
				
			} finally {
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public void dumpDatabaseTo(File file, String fromTimestamp) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("dumping database to " + file.getAbsolutePath());
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
	
				String query = 
							"attach database '" + file.getAbsolutePath() + "' as export;" +
							"create table export.archentity as select * from archentity where aenttimestamp > '" + fromTimestamp + "';" +
							"create table export.aentvalue as select * from aentvalue where valuetimestamp > '" + fromTimestamp + "';" +
							"create table export.aentreln as select * from aentreln where aentrelntimestamp > '" + fromTimestamp + "';" +
							"create table export.relationship as select * from relationship where relntimestamp > '" + fromTimestamp + "';" +
							"create table export.relnvalue as select * from relnvalue where relnvaluetimestamp > '" + fromTimestamp + "';" +
							"detach database export;";
				db.exec(query, createCallback());
				
			} finally {
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public static void debugDump(File file) {
		jsqlite.Database db = null;
		try {
			
			db = new jsqlite.Database();
			db.open(file.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
			
			db.exec("select * from archentity;", new Callback() {
				@Override
				public void columns(String[] coldata) {
					FLog.d("Columns: " + Arrays.toString(coldata));
				}

				@Override
				public void types(String[] types) {
					FLog.d("Types: " + Arrays.toString(types));
				}

				@Override
				public boolean newrow(String[] rowdata) {
					FLog.d("Row: " + Arrays.toString(rowdata));

					return false;
				}
			});
			
		} catch (Exception e) {
			FLog.e("error dumping database", e);
		} finally {
			try {
				if (db != null) {
					db.close();
					db = null;
				}
			} catch (Exception e) {
				FLog.e("error closing database", e);
			}
		}
	}

	public boolean isEmpty(File file) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("checking if database " + file.getAbsolutePath() + " is empty");
			try {
				
				db = new jsqlite.Database();
				db.open(file.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
				if (!isTableEmpty(db, "archentity")) return false;
				if (!isTableEmpty(db, "aentvalue")) return false;
				if (!isTableEmpty(db, "relationship")) return false;
				if (!isTableEmpty(db, "relnvalue")) return false;
				if (!isTableEmpty(db, "aentreln")) return false;
				
				return true;
			} finally {
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	private boolean isTableEmpty(jsqlite.Database db, String table) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(*) from " + table + ";");
			st.step();
			int count = st.column_int(0);
			if (count == 0) {
				return true;
			}
			return false;
		} finally {
			if (st != null) st.close();
		}
	}
	
	public void mergeDatabaseFrom(File file) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("merging database");
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				String query = 
						"attach database '" + file.getAbsolutePath() + "' as import;" +
						"insert into archentity (uuid, aenttimestamp, userid, doi, aenttypeid, geospatialcolumntype, geospatialcolumn, deleted) select uuid, aenttimestamp, userid, doi, aenttypeid, geospatialcolumntype, geospatialcolumn, deleted from import.archentity where uuid || aenttimestamp not in (select uuid || aenttimestamp from archentity);" +
						"insert into aentvalue (uuid, valuetimestamp, vocabid, attributeid, freetext, measure, certainty) select uuid, valuetimestamp, vocabid, attributeid, freetext, measure, certainty from import.aentvalue where uuid || valuetimestamp || attributeid not in (select uuid || valuetimestamp||attributeid from aentvalue);" +
						"insert into relationship (relationshipid, userid, relntimestamp, geospatialcolumntype, relntypeid, geospatialcolumn, deleted) select relationshipid, userid, relntimestamp, geospatialcolumntype, relntypeid, geospatialcolumn, deleted from import.relationship where relationshipid || relntimestamp not in (select relationshipid || relntimestamp from relationship);" +
						"insert into relnvalue (relationshipid, attributeid, vocabid, relnvaluetimestamp, freetext, certainty) select relationshipid, attributeid, vocabid, relnvaluetimestamp, freetext, certainty from import.relnvalue where relationshipid || relnvaluetimestamp || attributeid not in (select relationshipid || relnvaluetimestamp || attributeid from relnvalue);" + 
						"insert into aentreln (uuid, relationshipid, participatesverb, aentrelntimestamp, deleted) select uuid, relationshipid, participatesverb, aentrelntimestamp, deleted from import.aentreln where uuid || relationshipid || aentrelntimestamp not in (select uuid || relationshipid || aentrelntimestamp from aentreln);" +
						"detach database import;";
				db.exec(query, createCallback());
			} finally {
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
}
