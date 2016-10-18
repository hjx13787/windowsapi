package com.menage.jnative.util;

import org.xvolks.jnative.JNative;
import org.xvolks.jnative.Type;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.misc.basicStructures.HANDLE;
import org.xvolks.jnative.pointers.Pointer;
import org.xvolks.jnative.util.Kernel32;

public class Kernel32Utils {
	
	public static int VirtualQueryEx(int hProcess, int lpAddress,MEMORY_BASIC_INFORMATION lpBuffer) throws NativeException, IllegalAccessException{
		JNative j = new JNative(Kernel32.DLL_NAME, "VirtualQueryEx");
		j.setRetVal(Type.INT);
		j.setParameter(0, hProcess);
		j.setParameter(1, lpAddress);
		j.setParameter(2, lpBuffer.getPointer());
		j.setParameter(3, lpBuffer.getSizeOf());
		j.invoke();
		lpBuffer = lpBuffer.getValueFromPointer();
		return j.getRetValAsInt();
	}
	/**
	 * 读取内存 
	 * HANDLE hProcess, 
	 * LPCVOID lpBaseAddress, 
	 * LPVOID lpBuffer, 
	 * SIZE_T nSize, 
	 * SIZE_T* lpNumberOfBytesRead
	 * 
	 * @param hProcess 进程 通过Kernel32.OpenProcess(Kernel32.PROCESS_ALL_ACCESS, true, User32.GetWindowThreadProcessId());获得
	 * @param lpBaseAddress  地址
	 * @param lpBuffer 读取内存内容指针
	 * @param len 内存长度
	 * @param lpNumberOfBytesRead 输出的长度
	 * @return
	 * @throws NativeException
	 * @throws IllegalAccessException
	 */
	public static boolean ReadProcessMemory(HANDLE hProcess, int lpBaseAddress, Pointer lpBuffer, int len, Pointer lpNumberOfBytesRead) throws NativeException, IllegalAccessException {
		JNative gms = new JNative(Kernel32.DLL_NAME, "ReadProcessMemory");
		gms.setRetVal(Type.INT);

		int i = 0;
		gms.setParameter(i++, hProcess.getValue());
		gms.setParameter(i++, lpBaseAddress);
		gms.setParameter(i++, lpBuffer);
		gms.setParameter(i++, len);
		gms.setParameter(i++, lpNumberOfBytesRead);

		gms.invoke();

		i = gms.getRetValAsInt();

		return (i != 0);
	}
}
