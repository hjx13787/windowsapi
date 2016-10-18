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
	 * ��ȡ�ڴ� 
	 * HANDLE hProcess, 
	 * LPCVOID lpBaseAddress, 
	 * LPVOID lpBuffer, 
	 * SIZE_T nSize, 
	 * SIZE_T* lpNumberOfBytesRead
	 * 
	 * @param hProcess ���� ͨ��Kernel32.OpenProcess(Kernel32.PROCESS_ALL_ACCESS, true, User32.GetWindowThreadProcessId());���
	 * @param lpBaseAddress  ��ַ
	 * @param lpBuffer ��ȡ�ڴ�����ָ��
	 * @param len �ڴ泤��
	 * @param lpNumberOfBytesRead ����ĳ���
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
