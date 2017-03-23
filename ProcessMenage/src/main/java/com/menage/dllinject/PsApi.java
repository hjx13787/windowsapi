package com.menage.dllinject;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface PsApi extends Library {
	PsApi INSTANCE=(PsApi) Native.loadLibrary("Psapi", PsApi.class);
	
	int EnumProcesses(int[] pProcessIds,int cb,int[] pBytesReturned);
	
	int EnumProcessModules(int hProcess,int[] lphModule,int cb,int[] lpcbNeeded);
	
	int EnumProcessModulesEx(int  hProcess,int[] lphModule,int cb, int[] lpcbNeeded,int dwFilterFlag);

}
