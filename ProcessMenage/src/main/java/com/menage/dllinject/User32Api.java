package com.menage.dllinject;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface User32Api extends Library {
	User32Api INSTANCE = (User32Api) Native.loadLibrary("user32", User32Api.class);

	public int FindWindow(String lpClassName, String lpWindowName);

	public int GetWindowThreadProcessId(int hwnd, int ID);
}
