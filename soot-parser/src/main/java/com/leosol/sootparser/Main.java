package com.leosol.sootparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.RuntimeErrorException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.xmlpull.v1.XmlPullParserException;

import com.leosol.sootparser.metadata.AndroidManifest;
import com.leosol.sootparser.usage.UsageSearchResult;
import com.leosol.sootparser.usage.UsageSearchService;
import com.leosol.sootparser.usage.UsageSearchSpec;
import com.leosol.sootparser.util.AndroidUtil;
import com.leosol.sootparser.util.InstrumentUtil;

import soot.Scene;
import soot.SootClass;
import soot.Value;
import soot.jimple.AbstractConstantSwitch;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.internal.JimpleLocal;

public class Main {

	private static Set<String> ignoredFiles = new TreeSet<String>();

	public static void main(String[] args) {
		String androidSdk = null;
		if (System.getenv().containsKey("ANDROID_HOME")) {
			androidSdk = System.getenv("ANDROID_HOME") + File.separator + "platforms";
		} else {
			throw new IllegalStateException("Missing ANDROID_HOME variable!");
		}

		String inputDir = System.getProperty("user.dir") + File.separator + "input" + File.separator;
		String outputDir = System.getProperty("user.dir") + File.separator + "output" + File.separator;

		PrintWriter ctrlFile = createCtrlFile(outputDir);
		CSVPrinter csvPrinter = createCSVFile(outputDir);
		List<Path> filesToProcess = getFilesToProcess(inputDir);

		List<AndroidManifest> manifests = new LinkedList<AndroidManifest>();
		int i = 0;
		for (Path path : filesToProcess) {
			if (path.toString().toLowerCase().endsWith(".apk")) {
				String processedFile = null;
				processedFile = path.toString();
				try {
					File f = path.toFile();
					System.out.println("Processing " + (++i) + " of " + filesToProcess.size() + ": " + path);
					AndroidManifest am = null;
					try {
						am = readManifestInfo(f.getPath());
					} catch (AssertionError e) {

					}
					if (am != null) {
						am.setApkName(f.getName());
						manifests.add(am);
						if (!ignoredFiles.contains(path.toString())) {
							processApk(androidSdk, am, outputDir, csvPrinter);
							processedFile = path.toString();
						}
					}
				} catch (Exception e) {
					// e.printStackTrace();
				} finally {
					if (processedFile != null) {
						ignoredFiles.add(processedFile);
						ctrlFile.println(processedFile);
						ctrlFile.flush();
					}

				}
			}
		}
		try {
			csvPrinter.flush();
			csvPrinter.close();
			writeAndroidManifests(outputDir, manifests);
		} catch (IOException io) {
			throw new RuntimeException(io);
		}
	}

	private static List<Path> getFilesToProcess(String inputDir) {
		List<Path> filesToProcess = new LinkedList<Path>();
		try {
			Files.find(Paths.get(inputDir), Integer.MAX_VALUE,
					(filePath, fileAttr) -> filePath.toString().toLowerCase().endsWith(".apk"))
					.forEach(x -> filesToProcess.add(x));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return filesToProcess;
	}

	private static PrintWriter createCtrlFile(String outputPath) {
		try {
			File f = new File(outputPath + "/ctrlfile.txt");
			if (f.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				String line = null;
				while ((line = reader.readLine()) != null) {
					ignoredFiles.add(line.trim());
				}
				reader.close();
			}
			PrintWriter ctrlFile;

			ctrlFile = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
			return ctrlFile;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static void processApk(String androidSdk, AndroidManifest am, String outputPath, CSVPrinter csvPrinter)
			throws IOException {
		System.gc();
		InstrumentUtil.setupSoot(androidSdk, am.getApkPath(), outputPath);
		UsageSearchService service = new UsageSearchService();
		service.addSearchSpec(createInvokeSetNumUpdatesSpec());
		service.addSearchSpec(createInvokeSetPrioritySpec());
		service.addSearchSpec(createInvokeSetIntervalSpec());
		service.addSearchSpec(createInvokeSetExpirationDurationSpec());
		service.addSearchSpec(createInvokeRequestLocationUpdatesSpec());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates0());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates1());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates2());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates3());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates4());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates5());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates6());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates7());
		service.addSearchSpec(createInvokeLMrequestLocationUpdates8());
		service.addSearchSpec(createInvokeLMrequestSingleUpdate());
		service.addSearchSpec(createInvokeLMgetLastKnownLocation());
		service.addSearchSpec(createInvokeAnyMethodWithLocationRequest());
		service.addSearchSpec(createInvokeAnyMethodWithNameRequestLocationUpdates());

		for (SootClass clazz : Scene.v().getApplicationClasses()) {
			if (AndroidUtil.isAndroidClass(clazz)) {
				continue;
			}
			service.findUsages(clazz);
		}
		List<UsageSearchSpec> specs = service.getSearchSpecs();

		for (UsageSearchSpec spec : specs) {
//			System.out.println(spec.getClassSignature());
//			System.out.println(spec.getMethodSignature());
			List<UsageSearchResult> results = spec.getSearchResults();
			for (UsageSearchResult res : results) {
//				System.out.println("\t" + res.getSootClass());
//				System.out.println("\t" + res.getMethod());
//				System.out.println("\t" + res.getUnit());
//				System.out.println("\t" + res.getArguments());
				List<Value> argsValues = res.getArguments();
				String[] csvParsedArgs = { "", "", "", "" };
				int i = 0;
				for (Value v : argsValues) {
					if (!(i < csvParsedArgs.length)) {
						break;
					}
					AtomicReference<String> strValue = new AtomicReference<String>();
					if (v instanceof JimpleLocal) {
						strValue.set("Variable");
					} else {
						strValue.set("Not a Constant: " + v);
						v.apply(new AbstractConstantSwitch() {
							@Override
							public void caseDoubleConstant(DoubleConstant v) {
								strValue.set("" + v.value);
							}

							@Override
							public void caseFloatConstant(FloatConstant v) {
								strValue.set("" + v.value);
							}

							@Override
							public void caseIntConstant(IntConstant v) {
								strValue.set("" + v.value);
							}

							@Override
							public void caseLongConstant(LongConstant v) {
								strValue.set("" + v.value);
							}

							@Override
							public void caseNullConstant(NullConstant v) {
								strValue.set("");
							}

							@Override
							public void caseStringConstant(StringConstant v) {
								strValue.set("" + v.value);
							}
						});
					}
					csvParsedArgs[i] = strValue.get();
					i++;
				}
				csvPrinter.printRecord(am.getApkName(), am.getPackageName(), spec.getClassSignature(),
						spec.getMethodSignature(), res.getSootClass(), res.getMethod().getSubSignature(),
						csvParsedArgs[0], csvParsedArgs[1], csvParsedArgs[2], csvParsedArgs[3], res.getUnit());

			}
			csvPrinter.flush();
		}
	}

	public static void writeAndroidManifests(String outputPath, List<AndroidManifest> manifests) throws IOException {

		String[] permissionHeaders = { "APK", "APKPackage", "Permission" };
		CSVPrinter permissionsPrinter = createOrOpenCSV(outputPath, "/apk_permissions.csv", permissionHeaders);

		String[] featuresHeaders = { "APK", "APKPackage", "Feature" };
		CSVPrinter featuresPrinter = createOrOpenCSV(outputPath, "/apk_features.csv", featuresHeaders);

		String[] generalInfo = { "APK", "APKPackage", "minSdkVersion", "targetSdkVersion", "appName" };
		CSVPrinter generalInfoPrinter = createOrOpenCSV(outputPath, "/apk_general_info.csv", generalInfo);

		for (AndroidManifest am : manifests) {
			generalInfoPrinter.printRecord(am.getApkName(), am.getPackageName(), am.getMinSdkVersion(),
					am.getTargetSdkVersion(), am.getAppName());
			for (String p : am.getUsedPermitions()) {
				permissionsPrinter.printRecord(am.getApkName(), am.getPackageName(), p);
			}
			for (String f : am.getUsedFeatures()) {
				featuresPrinter.printRecord(am.getApkName(), am.getPackageName(), f);
			}
		}
		permissionsPrinter.close();
		featuresPrinter.close();
		generalInfoPrinter.close();
	}

	private static CSVPrinter createOrOpenCSV(String outputPath, String filename, String[] headers) throws IOException {
		CSVPrinter printer;
		File file = new File(outputPath + filename);
		if (file.exists()) {
			FileWriter fWriter = new FileWriter(file, true);
			printer = new CSVPrinter(fWriter, CSVFormat.DEFAULT);
		} else {
			FileWriter outPermissions = new FileWriter(file);
			printer = new CSVPrinter(outPermissions, CSVFormat.DEFAULT.withHeader(headers));
		}
		return printer;
	}

	public static CSVPrinter createCSVFile(String outputPath) {
		try {
			String[] csvHeaders = { "APK", "APKPackage", "ClassToSearch", "MethodToSearch", "FoundAtClass",
					"FoundAtMethod", "Arguments0", "Arguments1", "Arguments2", "Arguments3", "Unit", };
			CSVPrinter printer = createOrOpenCSV(outputPath, "/soot_parser.csv", csvHeaders);
			return printer;
		} catch (IOException io) {
			throw new RuntimeException(io);
		}
	}

	public static AndroidManifest readManifestInfo(String apk) {
		AndroidManifest result = new AndroidManifest();
		result.setApkPath(apk);
		File apkFile = new File(apk);
		ProcessManifest pm;
		try {
			pm = new ProcessManifest(apkFile);

			result.setTargetSdkVersion("" + pm.targetSdkVersion());
			result.setMinSdkVersion("" + pm.getMinSdkVersion());
			result.setPackageName(pm.getPackageName());
			result.getUsedPermitions().addAll(pm.getPermissions());
			result.setAppName(pm.getApplicationName());
			AXmlHandler axmlh = pm.getAXml();
			List<AXmlNode> features = axmlh.getNodesWithTag("uses-feature");
			Iterator<AXmlNode> featuresIt = features.iterator();
			while (featuresIt.hasNext()) {
				AXmlNode itNode = featuresIt.next();
				if (itNode.getAttribute("name") != null) {
					result.getUsedFeatures().add("" + itNode.getAttribute("name").getValue());
				}
			}
			pm.close();
			return result;
		} catch (IOException | XmlPullParserException e) {
			throw new RuntimeException(e);
		}
	}

	public static UsageSearchSpec createInvokeSetNumUpdatesSpec() {
		String methodSignature = "com.google.android.gms.location.LocationRequest setNumUpdates(int)";
		String classSignature = "com.google.android.gms.location.LocationRequest";
		return new UsageSearchSpec(methodSignature, classSignature, 0);
	}

	public static UsageSearchSpec createInvokeSetPrioritySpec() {
		String methodSignature = "com.google.android.gms.location.LocationRequest setPriority(int)";
		String classSignature = "com.google.android.gms.location.LocationRequest";
		return new UsageSearchSpec(methodSignature, classSignature, 0);
	}

	public static UsageSearchSpec createInvokeSetIntervalSpec() {
		String methodSignature = "com.google.android.gms.location.LocationRequest setInterval(long)";
		String classSignature = "com.google.android.gms.location.LocationRequest";
		return new UsageSearchSpec(methodSignature, classSignature, 0);
	}

	public static UsageSearchSpec createInvokeSetExpirationDurationSpec() {
		String methodSignature = "com.google.android.gms.location.LocationRequest setExpirationDuration(long)";
		String classSignature = "com.google.android.gms.location.LocationRequest";
		return new UsageSearchSpec(methodSignature, classSignature, 0);
	}

	public static UsageSearchSpec createInvokeRequestLocationUpdatesSpec() {
		String methodSignature = "com.google.android.gms.tasks.Task requestLocationUpdates(com.google.android.gms.location.LocationRequest,android.app.PendingIntent)";
		String classSignature = "com.google.android.gms.location.FusedLocationProviderClient";
		return new UsageSearchSpec(methodSignature, classSignature);
	}

	// LocationManager
	public static UsageSearchSpec createInvokeLMrequestLocationUpdates0() {
		String methodSignature = "void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, 1, 2);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates1() {
		String methodSignature = "void requestLocationUpdates(long,float,Criteria,android.location.LocationListener,android.os.Looper)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, 0, 1);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates2() {
		String methodSignature = "void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, 1, 2);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates3() {
		String methodSignature = "void requestLocationUpdates(java.lang.String,long,float,java.util.concurrent.Executor,android.location.LocationListener)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, 1, 2);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates4() {
		String methodSignature = "void requestLocationUpdates(java.lang.String,android.location.LocationRequest,android.app.PendingIntent)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates5() {
		String methodSignature = "void requestLocationUpdates(java.lang.String,android.location.LocationRequest,java.util.concurrent.Executor,android.location.LocationListener)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates6() {
		String methodSignature = "void requestLocationUpdates(long,float,Criteria,android.app.PendingIntent)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, 0, 1);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates7() {
		String methodSignature = "void requestLocationUpdates(long,float,Criteria,java.util.concurrent.Executor,android.location.LocationListener)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, 0, 1);
	}

	public static UsageSearchSpec createInvokeLMrequestLocationUpdates8() {
		String methodSignature = "void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, 1, 2);
	}

	public static UsageSearchSpec createInvokeLMrequestSingleUpdate() {
		String methodSignature = "void requestSingleUpdate\\(.*\\)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, false, true);
	}

	public static UsageSearchSpec createInvokeLMgetLastKnownLocation() {
		String methodSignature = "void getLastKnownLocation\\(.*\\)";
		String classSignature = "android.location.LocationManager";
		return new UsageSearchSpec(methodSignature, classSignature, false, true);
	}

	public static UsageSearchSpec createInvokeAnyMethodWithLocationRequest() {
		String methodSignature = "void .*\\(.*android.location.LocationRequest.*\\)";
		String classSignature = ".*";
		return new UsageSearchSpec(methodSignature, classSignature, true, true);
	}

	public static UsageSearchSpec createInvokeAnyMethodWithNameRequestLocationUpdates() {
		String methodSignature = "void requestLocationUpdates\\(.*\\)";
		String classSignature = ".*";
		return new UsageSearchSpec(methodSignature, classSignature, true, true);
	}

}
