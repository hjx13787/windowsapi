package com.menage;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.xvolks.jnative.JNative;
import org.xvolks.jnative.Type;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.misc.basicStructures.AbstractBasicData;
import org.xvolks.jnative.misc.basicStructures.HANDLE;
import org.xvolks.jnative.misc.basicStructures.HWND;
import org.xvolks.jnative.pointers.Pointer;
import org.xvolks.jnative.pointers.memory.MemoryBlockFactory;
import org.xvolks.jnative.util.Kernel32;
import org.xvolks.jnative.util.User32;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class MemoryQuery {

	protected Shell shell;
	private Text text;
	int lpAddress;
	public static int v = 0;
	public int MEM_COMMIT = 0x1000; // 已物理分配
	public int MEM_PRIVATE = 0x20000;
	public int PAGE_READWRITE = 0x04; // 可读写内存
	List<MemoryInfo> listSearch=new ArrayList<>();
	long searchValue=0;
	private Table table;
	int totalsize=0;
	private HWND handle;
	
	public class MemoryInfo{
		String address;
		String value;
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MemoryQuery window = new MemoryQuery();
			window.open(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open(int handle) {
		if (handle>0) {
			this.handle=new HWND(handle);
		}
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("SWT Application");
		shell.setLayout(new GridLayout(2, false));

		Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new FillLayout(SWT.HORIZONTAL));
		GridData gd_composite_1 = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd_composite_1.widthHint = 209;
		composite_1.setLayoutData(gd_composite_1);
		
		table = new Table(composite_1, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tableColumn = new TableColumn(table, SWT.NONE);
		tableColumn.setWidth(100);
		tableColumn.setText("\u5730\u5740");
		
		TableColumn tableColumn_1 = new TableColumn(table, SWT.NONE);
		tableColumn_1.setWidth(100);
		tableColumn_1.setText("\u503C");
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		Button button = new Button(composite, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					searchValue=Long.parseLong(text.getText());
//					handle = new HWND(shell.handle);
					int getWindowThreadProcessId = User32.GetWindowThreadProcessId(handle);
					//
					HANDLE openProcess = Kernel32.OpenProcess(Kernel32.PROCESS_ALL_ACCESS, true, getWindowThreadProcessId);

					System.out.println("openProcess==" + openProcess);
					lpAddress = 0x0000000;
					MEMORY_BASIC_INFORMATION lpBuffer = new MEMORY_BASIC_INFORMATION();
					int dwLength = lpBuffer.getSizeOf();
					int hProcess = openProcess.getValue();
					int i = 0;
					while (lpAddress >= 0 && lpAddress < 0x7fffffff && dwLength >= 0) {
						JNative j = new JNative(Kernel32.DLL_NAME, "VirtualQueryEx");
						j.setRetVal(Type.INT);
						j.setParameter(0, hProcess);
						j.setParameter(1, lpAddress);
						j.setParameter(2, lpBuffer.getPointer());
						j.setParameter(3, dwLength);
						j.invoke();
						lpBuffer = lpBuffer.getValueFromPointer();
						int retValAsInt = j.getRetValAsInt();
						int regionSize = lpBuffer.RegionSize;
						if (retValAsInt == dwLength) {
							int state = lpBuffer.State;
//							System.out.println(i + "==retValAsInt=" + retValAsInt + "===lpAddress=" + lpAddress + "===========RegionSize=" + regionSize + "=State=" + state);
							if (state == MEM_COMMIT && lpBuffer.Protect == PAGE_READWRITE) {
								int lpBaseAddress = lpAddress;
								Pointer lpBuffer2 = new Pointer(MemoryBlockFactory.createMemoryBlock(regionSize));
								Pointer lpNumberOfBytesRead = new Pointer(MemoryBlockFactory.createMemoryBlock(4));
								ReadProcessMemory(openProcess, lpBaseAddress, lpBuffer2, regionSize, lpNumberOfBytesRead);
								int asInt = lpNumberOfBytesRead.getAsInt(0);
								if (asInt==regionSize) {
									firstSearchData(lpBuffer2.getMemory());
								}
							}
						} else {
							break;
						}
						lpAddress += regionSize;
						lpBuffer = new MEMORY_BASIC_INFORMATION();
						i++;
					}
					System.out.println(i+"====totalsize======="+totalsize);
					System.exit(0);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		button.setBounds(10, 51, 80, 27);
		button.setText("\u9996\u6B21\u67E5\u627E");

		text = new Text(composite, SWT.BORDER);
		text.setBounds(10, 10, 73, 23);

		Button btnNewButton = new Button(composite, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				v++;
				text.setText(v + "");
			}
		});
		btnNewButton.setBounds(95, 51, 80, 27);
		btnNewButton.setText("\u7EE7\u7EED\u67E5\u627E");
	}

	protected void firstSearchData(byte[] byData) {
		long num = 0;       //内存中读取上来的相应字节数据的值
        int typeOfByte=2;
		for (int i = 0, len = byData.length - typeOfByte; i < len; i++) {
            num = byData[i + typeOfByte - 1];
            for (int j = typeOfByte,k = 2; j > 1; j--,k++) {
                num = num << 8;
                num = num | byData[i + typeOfByte - k];
            }
            if (num == searchValue)
            AddToList(lpAddress + i, num);  //满足相应条件把地址和值保存起来
        }
	}

	private void AddToList(int i, long num) {
//		MemoryInfo mi=new MemoryInfo();
//		mi.address=Integer.toHexString(i);
//		mi.value=num+"";
//		listSearch.add(mi);
		totalsize++;
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

	public class MEMORY_BASIC_INFORMATION extends AbstractBasicData<MEMORY_BASIC_INFORMATION> {
		protected MEMORY_BASIC_INFORMATION() {
			super(null);
			try {
				createPointer();
			} catch (NativeException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		int BaseAddress; // 区域基地址。
		int AllocationBase; // 分配基地址。
		int AllocationProtect; // 区域被初次保留时赋予的保护属性。
		int RegionSize; // 区域大小（以字节为计量单位）。
		int State; // 状态（MEM_FREE、MEM_RESERVE或 MEM_COMMIT）。
		int Protect; // 保护属性。
		int Type; // 类型。

		@Override
		public MEMORY_BASIC_INFORMATION getValueFromPointer() throws NativeException {
			BaseAddress = getNextInt();
			AllocationBase = getNextInt();
			AllocationProtect = getNextInt();
			RegionSize = getNextInt();
			State = getNextInt();
			Protect = getNextInt();
			Type = getNextInt();
			return this;
		}

		@Override
		public int getSizeOf() {
			return 28;
		}

		@Override
		public Pointer createPointer() throws NativeException {
			pointer = new Pointer(MemoryBlockFactory.createMemoryBlock(getSizeOf()));
			return pointer;
		}
	}
}
