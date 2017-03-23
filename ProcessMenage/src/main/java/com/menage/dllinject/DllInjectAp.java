package com.menage.dllinject;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.xvolks.jnative.Convention;
import org.xvolks.jnative.JNative;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.misc.basicStructures.DWORD;
import org.xvolks.jnative.misc.basicStructures.HANDLE;
import org.xvolks.jnative.pointers.Pointer;
import org.xvolks.jnative.util.Kernel32;
import org.xvolks.jnative.util.PsAPI;
import org.xvolks.jnative.util.User32;

import com.menage.dllinject.Tlhelp32Api.LPMODULEENTRY32;
import com.sun.jna.platform.win32.NtDll;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.DND;

public class DllInjectAp {

	protected Shell shlDll;
	private Text text;
	private Text text_1;
	private Text text_2;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DllInjectAp window = new DllInjectAp();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shlDll.open();
		shlDll.layout();
		while (!shlDll.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlDll = new Shell();
		shlDll.setSize(450, 300);
		shlDll.setText("dll\u6CE8\u5165");
		shlDll.setLayout(new GridLayout(1, false));
		
		Composite composite = new Composite(shlDll, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1));
		
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label.setText("\u6CE8\u5165\u8FDB\u7A0B");
		
		text = new Text(composite, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_text.widthHint = 142;
		text.setLayoutData(gd_text);
		
		Button button = new Button(composite, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getProcesss();
			}
		});
		button.setText("\u9009\u62E9");
		
		Label lbldll = new Label(composite, SWT.NONE);
		lbldll.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lbldll.setText("DLL\u4F4D\u7F6E");
		
		text_1 = new Text(composite, SWT.BORDER);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		DropTarget dropTarget = new DropTarget(text_1, DND.DROP_MOVE);
		dropTarget.setTransfer(new Transfer[]{FileTransfer.getInstance()});
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				String[] s=(String[]) event.data; 
				for (String string : s) {
//					User32.CallWindowProc(lpPrevWndFunc, hwnd, msg, wparam, lparam)
					if (string.endsWith(".dll")) {
						text_1.setText(string);
						break;
					}
				}
			}
		});
		
		Button button_1 = new Button(composite, SWT.NONE);
		button_1.setText("\u9009\u62E9");
		new Label(composite, SWT.NONE);
		
		Button btndll = new Button(composite, SWT.NONE);
		btndll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text2 = text.getText();
				try {
					Integer integer = Integer.valueOf(text2);
					injectDll(integer);
				} catch (NumberFormatException e1) {
					return;
				}
			}
		});
		btndll.setText("\u6CE8\u5165DLL");
		new Label(composite, SWT.NONE);
		
		Composite composite_1 = new Composite(shlDll, SWT.NONE);
		composite_1.setLayout(new FillLayout(SWT.HORIZONTAL));
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		text_2 = new Text(composite_1, SWT.BORDER | SWT.WRAP | SWT.MULTI);
		
		
	}
	/**
	 *  A:通过窗体的Title或者进程信息找到程序的句柄。
         B:获得进程句柄、分配内存。
         C:写入数据。
         D:创建线程执行。
	 */
	protected void injectDll(int PID) {
		// A：查找到目标程序
		// 主要用的API有：
		// [DllImport("user32.dll", EntryPoint = "FindWindow")]
		// private extern static IntPtr FindWindow(string lpClassName, string lpWindowName);
		//
		// [DllImport("User32.dll", CharSet = CharSet.Auto)]
		// public static extern int GetWindowThreadProcessId(IntPtr hwnd, out int ID);

		// 当然获得句柄的方法有很多，这里我就用这种方法做演示。
		//
		// 调用函数找到窗体(这里找的是窗体的)：

		// com.sun.jna.platform.win32.User32 user32 = com.sun.jna.platform.win32.User32.INSTANCE;
		// HWND hwnd = user32.FindWindow(null, "目标程序");
		// IntByReference lpdwProcessId = new IntByReference();
		// user32.GetWindowThreadProcessId(hwnd, lpdwProcessId);
		// 查找进程的ID

		// B：主要用的API有：
		// [DllImport("kernel32.dll")]
		// public static extern int OpenProcess(int dwDesiredAccess, bool bInheritHandle, int dwProcessId);
		//
		// [DllImport("kernel32.dll")]
		// public static extern int VirtualAllocEx(int hwnd, int lpaddress, int size, int type, int tect);

		// 调用函数获得操作的句柄
		String dllPath = text_1.getText();
		if (PID == 0 || dllPath == null || dllPath.isEmpty()) {
			return;
		}
		Kernel32Api kernel32Api = Kernel32Api.INSTANCE;
		int calcProcess = kernel32Api.OpenProcess(2 | 8 | 32, false, PID);
		setMessage(false,"打开线程：{} 获取到结果：{}",PID,calcProcess);
		
		// 分配内存空间，获得首地址

		int address = kernel32Api.VirtualAllocEx(calcProcess, 0, dllPath.length() + 1, 4096, 64);
		setMessage("申请到内存地址：{}({})", address,Integer.toHexString(address));

		// C： 主要用到的API是：
		// [DllImport("kernel32.dll")]
		// public static extern int WriteProcessMemory(int hwnd, int baseaddress, string buffer, int nsize, int filewriten);

		// 调用函数写入内存
		int writeProcessMemory = kernel32Api.WriteProcessMemory(calcProcess, address, dllPath, dllPath.length() + 1, 0);
		if (writeProcessMemory == 0) {
			System.out.println("写入内存失败！");
			return;
		}
		
		setMessage("将dll地址：{} 写入内存：{}",dllPath, writeProcessMemory);
		
		// D：主要用到的API有：
		// [DllImport("kernel32.dll")]
		// public static extern int GetModuleHandleA(string name);
		//
		// [DllImport("kernel32.dll")]
		// public static extern int GetProcAddress(int hwnd, string lpname);
		//
		// [DllImport("kernel32.dll")]
		// public static extern int CreateRemoteThread(int hwnd, int attrib, int size, int address, int par, int flags, int threadid);

		// 调用Kernel32 的LoadLibraryA 方法来加载咱们的DLL
		int createRemoteThread = kernel32Api.CreateRemoteThread(calcProcess, 0, 0, kernel32Api.GetProcAddress(kernel32Api.GetModuleHandleA("Kernel32"), "LoadLibraryA"), address, 0, 0);
		if (createRemoteThread == 0) {
			System.out.println("创建失败！");
		} else {
			setMessage("成功:"+address+"==createRemoteThread="+createRemoteThread);
			kernel32Api.WaitForSingleObject(createRemoteThread, 1000000);
			int getProcAddress = kernel32Api.GetProcAddress(createRemoteThread, "nInvoke");
			System.out.println("getProcAddress===="+getProcAddress);
		}
		kernel32Api.CloseHandle(calcProcess);
	}
	
	void setMessage(String s,Object... objects){
		setMessage(true,s,objects);
	}
	void setMessage(boolean isApend,String s,Object... objects){
		s=s.replace("{}", "%s");
		if (isApend) {
			text_2.append(String.format(s, objects)+"\n");
		}else
		text_2.setText(String.format(s, objects)+"\n");
	}

	protected void getProcesss() {
		try {
//			int[] enumProcess = PsAPI.EnumProcess(1024);
//			for (int i : enumProcess) {
//				DWORD nSize = new DWORD(0);
//				HANDLE openProcess = Kernel32.OpenProcess(2 | 8 | 32, true, i);
//				if (openProcess.getValue() == 0) {
//					continue;
//				}
//
//				Pointer enumProcessModules = PsAPI.EnumProcessModules(openProcess, 1024);
//				boolean null1 = enumProcessModules.isNull();
//				if (null1) {
//					System.out.println("Kernel32.GetLastError()==="+Kernel32.GetLastError());
//					continue;
//				}
//				String getModuleBaseName = PsAPI.GetModuleBaseName(openProcess, enumProcessModules.getPointer(), nSize.getPointer().getPointer());
//				System.out.println(i + "==" + openProcess + "==" + enumProcessModules + "==" + getModuleBaseName + "==" + Kernel32.GetLastError());
//				Kernel32.CloseHandle(openProcess);
//
//			}
			
			int[] pProcessIds = new int[1024];
			int[] pBytesReturned = new int[16];
			int enumProcesses = PsApi.INSTANCE.EnumProcesses(pProcessIds, 1024, pBytesReturned);
			
			for (int i = 0; i < pBytesReturned[0]/4; i++) {
				int j = pProcessIds[i];
				int openProcess = Kernel32Api.INSTANCE.OpenProcess(2|8|32, false, j);
				if (openProcess==0) {
					continue;
				}
//				kernel32.c
				
				int snapshot = Tlhelp32Api.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPMODULE, j);
				LPMODULEENTRY32 lpme=new LPMODULEENTRY32();
				int module32First = Tlhelp32Api.INSTANCE.Module32First(snapshot, lpme);
				System.out.println("snapshot="+snapshot+"=="+module32First+"=="+Kernel32.GetLastError());
				int[] lpcbNeeded = new int[10];
				int[] lphModule = new int[1024];
				int enumProcessModules = PsApi.INSTANCE.EnumProcessModulesEx(j, lphModule, 1024, lpcbNeeded,0x03);
				System.out.println("openProcess:"+openProcess+"====enumProcessModules:"+enumProcessModules+"==="+lpcbNeeded[0]+"=="+lphModule[0]+ "==" + Kernel32.GetLastError());
				System.out.println(i+"==="+enumProcesses+"=="+"==="+j);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * @throws Exception 
	 * 
	 */
	public void callFunction() throws Exception {
			MyJNative j = new MyJNative(0, "",Convention.DEFAULT);
	}
}
