package com.menage.dllinject;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Tlhelp32.PROCESSENTRY32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.win32.StdCallLibrary;

public interface Tlhelp32Api extends StdCallLibrary {
	Tlhelp32Api INSTANCE=(com.menage.dllinject.Tlhelp32Api) Native.loadLibrary("Kernel32", Tlhelp32Api.class);
	int CreateToolhelp32Snapshot(DWORD dwFlags,int th32ProcessID);
	int Module32First(int  hSnapshot,LPMODULEENTRY32 lpme);
	int Module32Next(int hSnapshot,LPMODULEENTRY32 lpme);
	
	public class LPMODULEENTRY32 extends Structure{
		public static class ByReference extends PROCESSENTRY32 implements Structure.ByReference {}

        public LPMODULEENTRY32() {
        	size = size();
        }

        public LPMODULEENTRY32(Pointer memory) {
            super(memory);
            read();
        }
        public int size;
        public int mid;
        public int pid;
        public int gusage;
        public int pusage;
        public int bases;
        public int bsize;
        public int hModule;
        public byte[] mFile=new byte[256];
        public byte[] mPath=new byte[256];
		@Override
		protected List getFieldOrder() {
			return Arrays.asList(new String[]{"size","mid","pid","gusage","pusage","bases","bsize","hModule","mFile","mPath"});
//			return Arrays.asList(new String[] { "dwSize", "th32ModuleID", "th32ProcessID", "GlblcntUsage", "ProccntUsage", "modBaseAddr", "modBaseSize", "hModule", "szModule", "szExePath" });
		}
	}
}
