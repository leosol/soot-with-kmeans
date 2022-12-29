from pathlib import Path
import subprocess
import os
import sqlite3


class CSVToSQLite:
    def __init__(self):
        self.csv_file = None
        self.db_file = None
        self.column_names = []

    def init(self, csv_file, db_file):
        if csv_file is None:
            self.csv_file = './csv-database.csv'
        else:
            self.csv_file = csv_file
        if db_file is None:
            self.db_file = "./csv-database.db"
        else:
            self.db_file = db_file

    def create_table(self, csv_file, db_file, table_name):
        first_line = ""
        with open(csv_file) as f:
            first_line = f.readline()
        con = sqlite3.connect(db_file)
        cur = con.cursor()
        cur.execute("CREATE TABLE IF NOT EXISTS " + table_name + " (" + first_line + ")")
        self.column_names = first_line.split(',')
        con.commit()
        con.close()

    def create_indexes(self, db_file, table_name):
        con = sqlite3.connect(db_file)
        cur = con.cursor()
        for column_name in self.column_names:
            str_idx_creation = "create index if not exists idx_" + column_name + " on " + table_name + "(" + column_name + ")"
            con.execute(str_idx_creation)
        con.commit()
        con.close()

    def create_views(self, db_file):
        c = sqlite3.connect(db_file)
        sql = """create view if not exists vw_static_analysis 
                as
                select 
                    APK,
                    APKPackage,
                    ClassToSearch, 
                    MethodToSearch,
                    FoundAtClass,
                    FoundAtMethod,
                    CASE MethodToSearch
                         WHEN 'void .*\(.*android.location.LocationRequest.*\)' THEN 'REF_REQ_OBJ'
                         WHEN 'void requestLocationUpdates\(.*\)' THEN 'REQ_LOCATION_METHOD_NAME'
                         WHEN 'void requestLocationUpdates(java.lang.String,android.location.LocationRequest,java.util.concurrent.Executor,android.location.LocationListener)' THEN 'REQ_LOC_UPDATE'
                         WHEN 'com.google.android.gms.tasks.Task requestLocationUpdates(com.google.android.gms.location.LocationRequest,android.app.PendingIntent)' THEN 'REQ_LOC_UPDATE'
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)' THEN 'REQ_LOC_UPDATE_WITH_PARAM'
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)' THEN 'REQ_LOC_UPDATE_WITH_PARAM'
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)' THEN 'REQ_LOC_UPDATE_WITH_PARAM'
                         WHEN 'void requestSingleUpdate\(.*\)' THEN 'SINGLE_REQUEST'
                         WHEN 'com.google.android.gms.location.LocationRequest setExpirationDuration(long)' THEN 'MAX_WAIT_FOR_LOCATION_MILLIS'
                         WHEN 'com.google.android.gms.location.LocationRequest setInterval(long)' THEN 'RECURRENCE_FOR_LOCATION_MILLIS'
                         WHEN 'com.google.android.gms.location.LocationRequest setNumUpdates(int)' THEN 'MAX_RECEIVED_LOCATIONS'
                         WHEN 'com.google.android.gms.location.LocationRequest setPriority(int)' THEN 'PRIORTY_ACCURATE_LP_PASSIVE'
                         ELSE 'UNKNOWN' 
                    END AS LOC_REQ_INFO,
                    CASE MethodToSearch
                         WHEN 'com.google.android.gms.location.LocationRequest setExpirationDuration(long)' THEN Arguments0
                         ELSE '' 
                    END AS MAX_WAIT_FOR_LOCATION_MILLIS,
                    CASE MethodToSearch
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)' THEN Arguments0
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)' THEN Arguments0
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)' THEN Arguments0
                         ELSE '' 
                    END AS MIN_TIME_BTW_UPDATES_MILLIS,
                    CASE MethodToSearch
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)' THEN Arguments1
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)' THEN Arguments1
                         WHEN 'void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)' THEN Arguments1
                         ELSE '' 
                    END AS MIN_DISTANCE_BTW_UPDATES_METERS,
                    CASE MethodToSearch
                         WHEN 'void requestSingleUpdate\(.*\)' THEN 'YES'
                         ELSE '' 
                    END AS IS_SINGLE_REQUEST,
                    CASE MethodToSearch
                         WHEN 'com.google.android.gms.location.LocationRequest setInterval(long)' THEN Arguments0
                         ELSE '' 
                    END AS REPEAT_INTERVAL,
                    CASE MethodToSearch
                         WHEN 'com.google.android.gms.location.LocationRequest setNumUpdates(int)' THEN Arguments0
                         ELSE '' 
                    END AS NUM_UPDATES,
                    CASE MethodToSearch
                         WHEN 'com.google.android.gms.location.LocationRequest setPriority(int)' THEN Arguments0
                         ELSE '' 
                    END AS PRIORITY
                from soot_parser_csv"""
        c.execute(sql)
        sql = """create view if not exists vw_expected_min_time
                as
                select 
                    APKPackage,
                    CASE 
                        WHEN CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)>-1.0 THEN CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)
                        WHEN MIN_TIME_BTW_UPDATES_MILLIS = 'Variable' THEN (SELECT AVG(CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)) FROM vw_static_analysis where CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)>0.0)
                        WHEN MIN_TIME_BTW_UPDATES_MILLIS = '' AND ( CAST(MIN_DISTANCE_BTW_UPDATES_METERS as decimal)>0.0 OR MIN_DISTANCE_BTW_UPDATES_METERS='Variable' ) THEN (SELECT AVG(CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)) FROM vw_static_analysis where CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)>0.0) 
                        WHEN IS_SINGLE_REQUEST = 'YES'  THEN (SELECT MAX(CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)) FROM vw_static_analysis where CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)>0.0) 
                        WHEN CAST(REPEAT_INTERVAL as decimal)>-1.0 THEN CAST(REPEAT_INTERVAL as decimal)
                        WHEN PRIORITY = '100' OR PRIORITY = '102' THEN (SELECT MIN(CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)) FROM vw_static_analysis where CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)>0.0)
                        WHEN PRIORITY = '104' OR PRIORITY = '105' THEN (SELECT MAX(CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)) FROM vw_static_analysis where CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)>0.0)
                    ELSE (SELECT MAX(CAST(MIN_TIME_BTW_UPDATES_MILLIS as decimal)) FROM vw_static_analysis) 
                    END AS EXPECTED_MIN_TIME_BTW_UPDATES
                from vw_static_analysis"""
        c.execute(sql)
        sql = """create view vw_expected_time_vs_permissions
                as 
                SELECT a.APKPackage, exp_time/1000 AS seconds, qtd_loc_permissions  FROM
                (	select 
                        APKPackage,
                        avg(EXPECTED_MIN_TIME_BTW_UPDATES) as exp_time
                    from vw_expected_min_time
                    GROUP BY 1 ) as a
                inner join 
                (	SELECT
                        APKPackage,
                        count(*) as qtd_loc_permissions
                    from apk_permissions_csv 
                    group by 1) as b
                on (a.APKPackage = b.APKPackage)
                where exp_time/1000 < 60*60*4
                order by 3"""
        c.execute(sql)
        c.commit()
        c.close()

    def run_subprocess(self, db_name, csv_file, table_name):
        result = subprocess.run(['sqlite3',
                                 str(db_name),
                                 '-cmd',
                                 '.mode csv',
                                 '.import --skip 1 ' + str(csv_file).replace('\\', '\\\\')
                                 + " " + table_name],
                                capture_output=True)
        print("SubProcess result: " + str(result))

    def create_db(self):
        db_name = Path(self.db_file).resolve()
        csv_file = Path(self.csv_file).resolve()
        preconditions = True
        if os.path.isfile(db_name):
            print("Database file exists " + str(db_name))
            print("Trying to append data")
            preconditions = True
        if preconditions and not os.path.isfile(csv_file):
            print("CSV file does not exists " + str(csv_file))
            print("There must exist a source for the data to be converted to sqlite")
            preconditions = False
        if preconditions:
            print("Creating database " + str(db_name) + " with data from " + str(csv_file))
            basename = os.path.basename(csv_file)
            table_name = basename.replace(' ', '_').replace('.', '_')
            self.create_table(csv_file, self.db_file, table_name)
            print("Calling subprocess sqlite3 - did you install it?")
            self.run_subprocess(db_name, csv_file, table_name)
            self.create_indexes(self.db_file, table_name)


if __name__ == '__main__':
    csv_files = ['..\\example-analysis\\apk_features.csv', '..\\example-analysis\\apk_general_info.csv',
                 '..\\example-analysis\\apk_permissions.csv', '..\\example-analysis\\soot_parser.csv']
    db_file = '..\\example-analysis\\example-analysis.db'
    for csv_f in csv_files:
        converter = CSVToSQLite()
        converter.init(csv_f, db_file)
        converter.create_db()
    converter.create_views(db_file)




