package com.menage;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.misc.basicStructures.HANDLE;
import org.xvolks.jnative.misc.basicStructures.HWND;
import org.xvolks.jnative.misc.basicStructures.LONG;
import org.xvolks.jnative.pointers.Pointer;
import org.xvolks.jnative.pointers.memory.MemoryBlockFactory;
import org.xvolks.jnative.util.Kernel32;
import org.xvolks.jnative.util.User32;

import com.menage.jnative.util.Kernel32Utils;
import com.menage.jnative.util.MEMORY_BASIC_INFORMATION;
import com.menage.jnative.util.StrUtil;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Label;

public class MemoryQuery {

	protected Shell shell;
	private Text text;
	int lpAddress;
	public static int v = 0;
	public int MEM_COMMIT = 0x1000; // 已物理分配
	public int MEM_PRIVATE = 0x20000;
	public int PAGE_READWRITE = 0x04; // 可读写内存
	List<MemoryInfo> listSearch = new ArrayList<>();
	long searchValue = 0;
	private Table table;
	int totalsize = 0;
	private HWND handle;
	private HANDLE openProcess;

	public class MemoryInfo {
		String address;
		String value;
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(2|2);
	}

	/**
	 * Open the window.
	 */
	public void open(int handle) {
		if (handle > 0) {
			try {
				this.handle = new HWND(handle);
				int getWindowThreadProcessId = User32.GetWindowThreadProcessId(this.handle);
				openProcess = Kernel32.OpenProcess(Kernel32.PROCESS_ALL_ACCESS, true, getWindowThreadProcessId);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		if (autoRefreshSearchMemory!=null) {
			autoRefreshSearchMemory.shutdown();
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

		lbl_msg = new Label(shell, SWT.NONE);
		lbl_msg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		new Label(shell, SWT.NONE);

		Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new FillLayout(SWT.HORIZONTAL));
		GridData gd_composite_1 = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd_composite_1.widthHint = 209;
		composite_1.setLayoutData(gd_composite_1);

		table = new Table(composite_1, SWT.BORDER | SWT.FULL_SELECTION);
		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selection = table.getSelection();
				if (StrUtil.isEmpty(selection)) {
					return;
				}
				TableItem tableItem = selection[0];
				String address = tableItem.getText(0);
				String value = tableItem.getText(01);
				txt_address.setText(address);
				txt_value.setText(value);
			}
		});
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
				if (button.getText().equals("首次查找")) {
					firstSearch();
					button.setText("重新查找");
				}else{
					listAddress.clear();
					listValue.clear();
					autoRefreshSearchMemory.shutdown();
					isRefresh=false;
					button.setText("首次查找");
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
				searchValue = Long.parseLong(text.getText());
				nextSearch();
			}
		});
		btnNewButton.setBounds(95, 51, 80, 27);
		btnNewButton.setText("\u7EE7\u7EED\u67E5\u627E");

		txt_address = new Text(composite, SWT.BORDER);
		txt_address.setBounds(17, 122, 73, 23);

		txt_value = new Text(composite, SWT.BORDER);
		txt_value.setBounds(102, 122, 73, 23);

		Button button_1 = new Button(composite, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				writeMemory();
			}
		});
		button_1.setBounds(95, 156, 80, 27);
		button_1.setText("\u5199\u5165\u5730\u5740");
		
		Button button_2 = new Button(composite, SWT.NONE);
		button_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				readMemory(txt_address.getText());
			}
		});
		button_2.setBounds(10, 156, 80, 27);
		button_2.setText("\u8BFB\u53D6\u5730\u5740");
	}

	protected void readMemory(String address) {
		try {
			int parseInt = Integer.parseInt(address, 16);
			Pointer lpBuffer = new Pointer(MemoryBlockFactory.createMemoryBlock(8));
			Pointer lpNumberOfBytesRead = new Pointer(MemoryBlockFactory.createMemoryBlock(4));
			Kernel32Utils.ReadProcessMemory(openProcess, parseInt, lpBuffer, lpBuffer.getSize(), lpNumberOfBytesRead);
			long asLong = lpBuffer.getAsLong(0);
			txt_value.setText(asLong+"");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void writeMemory() {
		try {
			String address = txt_address.getText();
			String value = txt_value.getText();
			Pointer lpBuffer = new Pointer(MemoryBlockFactory.createMemoryBlock(8));
			lpBuffer.setLongAt(0, Long.valueOf(value));
			boolean writeProcessMemory = Kernel32.WriteProcessMemory(openProcess, Integer.valueOf(address, 16), lpBuffer, lpBuffer.getSize());
			System.out.println(writeProcessMemory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 继续查询
	 */
	protected void nextSearch() {
		if (autoRefreshSearchMemory!=null) {
			autoRefreshSearchMemory.shutdown();
			isRefresh=false;
		}
		try {
			List<Integer> lista = new ArrayList<>();
			List<Long> listv = new ArrayList<>();
			for (Integer lpBaseAddress : listAddress) {
				Pointer lpBuffer2 = new Pointer(MemoryBlockFactory.createMemoryBlock(8));
				Pointer lpNumberOfBytesRead = new Pointer(MemoryBlockFactory.createMemoryBlock(4));
				Kernel32Utils.ReadProcessMemory(openProcess, lpBaseAddress, lpBuffer2, lpBuffer2.getSize(), lpNumberOfBytesRead);
				long asLong = lpBuffer2.getAsLong(0);
//				long asLong=getLong(lpBuffer2.getMemory());
				if (asLong == searchValue) {
					lista.add(lpBaseAddress);
					listv.add(asLong);
				}
			}
			listAddress = lista;
			listValue = listv;
			setTableValue();
			refreshSearch();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static long getLong(byte[] bb) { 
	       return ((((long) bb[ 0] & 0xff) << 56) 
	               | (((long) bb[ 1] & 0xff) << 48) 
	               | (((long) bb[ 2] & 0xff) << 40) 
	               | (((long) bb[ 3] & 0xff) << 32) 
	               | (((long) bb[ 4] & 0xff) << 24) 
	               | (((long) bb[ 5] & 0xff) << 16) 
	               | (((long) bb[ 6] & 0xff) << 8) | (((long) bb[ 7] & 0xff) << 0)); 
	  } 

	public void refreshSearch() {
		isRefresh=true;
		autoRefreshSearchMemory = Executors.newSingleThreadScheduledExecutor();
		autoRefreshSearchMemory.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					List<Integer> lista = new ArrayList<>();
					List<Long> listv = new ArrayList<>();
					for (Integer lpBaseAddress : listAddress) {
						if (!isRefresh) {
							return;
						}
						Pointer lpBuffer2 = new Pointer(MemoryBlockFactory.createMemoryBlock(8));
						Pointer lpNumberOfBytesRead = new Pointer(MemoryBlockFactory.createMemoryBlock(4));
						Kernel32Utils.ReadProcessMemory(openProcess, lpBaseAddress, lpBuffer2, lpBuffer2.getSize(), lpNumberOfBytesRead);
						long num = lpBuffer2.getAsLong(0);
						lista.add(lpBaseAddress);
						listv.add(num);
					}
					listAddress = lista;
					listValue = listv;
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							for (int i = 0; i < (listAddress.size() > 100 ? 100 : listAddress.size()); i++) {
								Integer integer = listAddress.get(i);
								String adr = StrUtil.padStart(Integer.toHexString(integer).toUpperCase(), 8, '0');
								String v = listValue.get(i).toString();
								if (table.isDisposed()) {
									break;
								}
								table.getItem(i).setText(new String[] { adr, v });
							}
							lbl_msg.setText("查找到" + listAddress.size() + "个数据" + (listAddress.size() > 100 ? "，显示100个" : ""));
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	protected void setTableValue() {
		TableItem[] items = table.getItems();
		for (TableItem tableItem : items) {
			tableItem.dispose();
		}
		for (int i = 0; i < (listAddress.size() > 100 ? 100 : listAddress.size()); i++) {
			Integer integer = listAddress.get(i);
			String adr = StrUtil.padStart(Integer.toHexString(integer).toUpperCase(), 8, '0');
			String v = listValue.get(i).toString();
			TableItem tableItem = new TableItem(table, SWT.NONE);
			tableItem.setText(new String[] { adr, v });
		}
		lbl_msg.setText("查找到" + listAddress.size() + "个数据" + (listAddress.size() > 100 ? "，显示100个" : ""));
	}

	protected void firstSearchData(byte[] byData) {
		long num = 0; // 内存中读取上来的相应字节数据的值
		int typeOfByte = 1 << 2;
		for (int i = 0, len = byData.length - typeOfByte; i < len; i++) {
			num = byData[i + typeOfByte - 1];
			for (int j = typeOfByte, k = 2; j > 1; j--, k++) {
				num = num << 8;
				num = num | byData[i + typeOfByte - k];
			}
			if (num == searchValue)
				AddToList(lpAddress + i, num); // 满足相应条件把地址和值保存起来
		}
	}

	List<Integer> listAddress = new ArrayList<>();
	List<Long> listValue = new ArrayList<>();
	private Label lbl_msg;
	private Text txt_address;
	private Text txt_value;
	private ScheduledExecutorService autoRefreshSearchMemory;
	private boolean isRefresh=true;

	private void AddToList(int i, long num) {
		listAddress.add(i);
		listValue.add(num);
		totalsize++;
	}

	/**
	 * 首次查询
	 */
	public void firstSearch() {
		try {
			if (autoRefreshSearchMemory != null) {
				autoRefreshSearchMemory.shutdown();
				isRefresh=false;
			}
			listAddress.clear();
			listValue.clear();
			searchValue = Long.parseLong(text.getText());
			// handle = new HWND(shell.handle);

			System.out.println("openProcess==" + openProcess);
			lpAddress = 0x0000000;
			MEMORY_BASIC_INFORMATION lpBuffer = new MEMORY_BASIC_INFORMATION();
			int dwLength = lpBuffer.getSizeOf();
			int hProcess = openProcess.getValue();
			int size=0;
			while (lpAddress >= 0 && lpAddress < 0x7fffffff && dwLength >= 0) {
				int retValAsInt = Kernel32Utils.VirtualQueryEx(hProcess, lpAddress, lpBuffer);
				int regionSize = lpBuffer.RegionSize;
				if (retValAsInt == dwLength) {
					int state = lpBuffer.State;
					// System.out.println(i + "==retValAsInt=" + retValAsInt + "===lpAddress=" + lpAddress + "===========RegionSize=" + regionSize + "=State=" + state);
					if (state == MEM_COMMIT && lpBuffer.Protect == PAGE_READWRITE) {
						int lpBaseAddress = lpAddress;
						Pointer lpBuffer2 = new Pointer(MemoryBlockFactory.createMemoryBlock(regionSize));
						Pointer lpNumberOfBytesRead = new Pointer(MemoryBlockFactory.createMemoryBlock(4));
						Kernel32Utils.ReadProcessMemory(openProcess, lpBaseAddress, lpBuffer2, regionSize, lpNumberOfBytesRead);
						int asInt = lpNumberOfBytesRead.getAsInt(0);
						if (asInt == regionSize) {
							firstSearchData(lpBuffer2.getMemory());
						}
					}
				} else {
					break;
				}
				lpAddress += regionSize;
				lpBuffer = new MEMORY_BASIC_INFORMATION();
			}
			setTableValue();
			refreshSearch();
			System.out.println("查询完成"+size);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
