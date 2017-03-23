package com.menage.dllinject;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface Kernel32Api extends Library {
	Kernel32Api INSTANCE=(Kernel32Api) Native.loadLibrary("Kernel32", Kernel32Api.class);
	
	public int OpenProcess(int dwDesiredAccess,boolean bInheritHandle, int dwProcessId);
	
	public int GetModuleHandleA(String name);
	 
	public int GetProcAddress(int hwnd, String lpname);
 
	public int CreateRemoteThread(int hwnd, int attrib, int size, int address, int par, int flags, int threadid);
	
	public int WriteProcessMemory(int hwnd, int baseaddress, String buffer, int nsize, int filewriten);
		    
	public int VirtualAllocEx(int hwnd, int lpaddress, int size, int type, int tect);
	
	int CloseHandle(int hObject);
	/**
	 * �ȴ�Զ���̹߳ر�
	 * @param hHandle CreateRemoteThread��ȡ�ý��
	 * @param dwMilliseconds  �ȴ�ʱ��
	 * @return
	 */
	int  WaitForSingleObject(int hHandle,long dwMilliseconds);
	
}
