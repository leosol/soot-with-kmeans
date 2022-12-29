package com.leosol.sootparser.metadata;


import java.util.LinkedList;
import java.util.List;

public class AndroidManifest {

	private String apkName;
	private String apkPath;
	private String minSdkVersion;
	private String targetSdkVersion;
	private String packageName;
	private String appLabel;
	private String appName;
	private List<String> usedPermitions = new LinkedList<String>();
	private List<String> usedFeatures = new LinkedList<String>();

	public String getMinSdkVersion() {
		return minSdkVersion;
	}

	public void setMinSdkVersion(String minSdkVersion) {
		this.minSdkVersion = minSdkVersion;
	}

	public String getTargetSdkVersion() {
		return targetSdkVersion;
	}

	public void setTargetSdkVersion(String targetSdkVersion) {
		this.targetSdkVersion = targetSdkVersion;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getAppLabel() {
		return appLabel;
	}

	public void setAppLabel(String appLabel) {
		this.appLabel = appLabel;
	}

	public List<String> getUsedPermitions() {
		return usedPermitions;
	}

	public void setUsedPermitions(List<String> usedPermitions) {
		this.usedPermitions = usedPermitions;
	}

	public List<String> getUsedFeatures() {
		return usedFeatures;
	}

	public void setUsedFeatures(List<String> usedFeatures) {
		this.usedFeatures = usedFeatures;
	}

	public String getApkPath() {
		return apkPath;
	}

	public void setApkPath(String apkPath) {
		this.apkPath = apkPath;
	}

	public String getApkName() {
		return apkName;
	}

	public void setApkName(String apkName) {
		this.apkName = apkName;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

}
