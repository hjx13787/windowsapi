package com.menage;


import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.misc.POINT;
import org.xvolks.jnative.misc.basicStructures.DC;
import org.xvolks.jnative.misc.basicStructures.HANDLE;
import org.xvolks.jnative.misc.basicStructures.HWND;
import org.xvolks.jnative.misc.basicStructures.LPARAM;
import org.xvolks.jnative.misc.basicStructures.LRECT;
import org.xvolks.jnative.misc.basicStructures.UINT;
import org.xvolks.jnative.misc.basicStructures.WPARAM;
import org.xvolks.jnative.pointers.Pointer;
import org.xvolks.jnative.util.Gdi32;
import org.xvolks.jnative.util.Kernel32;
import org.xvolks.jnative.util.User32;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.layout.RowLayout;

public class MainApp {

	protected Shell shell;
	private Text txt_title;
	private Text text_1;
	private HWND handle;
	private Text txt_handle;
	private Text text_class;
	private boolean isFindWindow = false;
	private Text text_X;
	private Text text_Y;
	private Text text_clientX;
	private Text text_clientY;
	private Text text_mouseColor;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println(10 << 16);
			MainApp window = new MainApp();
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
		shell.setSize(600, 355);
		shell.setText("SWT Application");
		GridLayout gl_shell = new GridLayout(1, false);
		gl_shell.verticalSpacing = 0;
		gl_shell.horizontalSpacing = 0;
		gl_shell.marginWidth = 0;
		shell.setLayout(gl_shell);

		Composite composite_2 = new Composite(shell, SWT.NONE);
		composite_2.setLayout(new RowLayout(SWT.HORIZONTAL));
		composite_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		Button button_3 = new Button(composite_2, SWT.NONE);
		button_3.addMouseListener(new MouseAdapter() {
			private Rectangle bounds;

			@Override
			public void mouseDown(MouseEvent e) {
				bounds = shell.getBounds();
				shell.setBounds(0, 0, bounds.width, bounds.height);
				isFindWindow = true;
			}

			@Override
			public void mouseUp(MouseEvent e) {
				shell.setBounds(bounds);
				isFindWindow = false;
			}
		});
		button_3.setText("取句柄");
		button_3.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				getWindowInfo();
			}
		});

		Button button_2 = new Button(composite_2, SWT.NONE);
		button_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO 获取客户区大小
				try {
					LRECT lpRect = new LRECT();
					boolean getClientRect = User32.GetClientRect(handle, lpRect);
					setText(getClientRect + "==" + lpRect.getTop() + "=" + lpRect.getLeft() + "====" + lpRect.getRight() + "==" + lpRect.getBottom());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		button_2.setText("获取客户区大小");

		Button btnNewButton = new Button(composite_2, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text2 = txt_title.getText();
				getHandle(text2);
				System.out.println(btnNewButton.handle);
			}
		});
		btnNewButton.setText("获取句柄");

		Button button = new Button(composite_2, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				leftClick(111, 555);
			}
		});
		button.setText("点击");
		Button button_1 = new Button(composite_2, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					getClientColor(Integer.valueOf(text_clientX.getText()), Integer.valueOf(text_clientY.getText()));
				} catch (NumberFormatException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
			}
		});
		button_1.setText("获取颜色");

		Button button_4 = new Button(composite_2, SWT.NONE);
		button_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				try {
					// System.out.println(User32.GetWindowThreadProcessId(new HWND(shell.handle)));
					// 关闭进程
					// int getWindowThreadProcessId = User32.GetWindowThreadProcessId(handle);
					int getWindowThreadProcessId = User32.GetWindowThreadProcessId(new HWND(shell.handle));
					//
					HANDLE openProcess = Kernel32.OpenProcess(Kernel32.PROCESS_ALL_ACCESS, true, getWindowThreadProcessId);
					System.out.println(openProcess);
					HANDLE getCurrentProcess = Kernel32.GetCurrentProcess();
					System.out.println(getCurrentProcess);
					Kernel32.TerminateProcess(openProcess, 0);
					// boolean closeHandle = Kernel32.CloseHandle(getCurrentProcess);
					// System.out.println(closeHandle);
					// com.sun.jna.platform.win32.User32 lib = com.sun.jna.platform.win32.User32.INSTANCE;
					// HOOKPROC arg1=new HOOKPROC() {
					// LRESULT callback(int nCode, WPARAM wParam, MOUSEHOOKSTRUCT lParam){
					// }
					// };
					//// com.sun.jna.win32.StdCallLibrary;
					// HINSTANCE arg2=new HINSTANCE();
					// lib.SetWindowsHookEx(1, arg1, arg2, 1);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		button_4.setText("关闭进程");
		
		Button button_5 = new Button(composite_2, SWT.NONE);
		button_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MemoryQuery mq=new MemoryQuery();
				mq.open(handle.getValue());
			}
		});
		button_5.setText("\u5185\u5B58\u4FEE\u6539");

		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		gl_composite.horizontalSpacing = 2;
		composite.setLayout(gl_composite);

		Label label_1 = new Label(composite, SWT.NONE);
		label_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label_1.setText("句柄");

		txt_handle = new Text(composite, SWT.BORDER);
		txt_handle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label.setText("标题");

		txt_title = new Text(composite, SWT.BORDER);
		txt_title.setText("TestMouseClick");
		txt_title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label label_2 = new Label(composite, SWT.NONE);
		label_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		label_2.setText("类名");

		text_class = new Text(composite, SWT.BORDER);
		text_class.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label label_3 = new Label(composite, SWT.NONE);
		label_3.setText("鼠标位置");

		Composite composite_3 = new Composite(composite, SWT.NONE);
		composite_3.setLayout(new GridLayout(6, false));
		composite_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblX = new Label(composite_3, SWT.NONE);
		lblX.setText("X");

		text_X = new Text(composite_3, SWT.BORDER);

		Label lblCx = new Label(composite_3, SWT.NONE);
		lblCx.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCx.setText("cX");

		text_clientX = new Text(composite_3, SWT.BORDER);
		text_clientX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblColor = new Label(composite_3, SWT.NONE);
		lblColor.setText("color");

		lbl_Color = new Label(composite_3, SWT.NONE);
		GridData gd_lbl_Color = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 2);
		gd_lbl_Color.heightHint = 50;
		gd_lbl_Color.widthHint = 50;
		lbl_Color.setLayoutData(gd_lbl_Color);

		Label lblY = new Label(composite_3, SWT.NONE);
		lblY.setText("Y");

		text_Y = new Text(composite_3, SWT.BORDER);

		Label lblCy = new Label(composite_3, SWT.NONE);
		lblCy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCy.setText("cY");

		text_clientY = new Text(composite_3, SWT.BORDER);
		text_clientY.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		text_mouseColor = new Text(composite_3, SWT.BORDER);
		text_mouseColor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Composite composite_1 = new Composite(shell, SWT.NONE);
		composite_1.setLayout(new FillLayout(SWT.HORIZONTAL));
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		text_1 = new Text(composite_1, SWT.BORDER | SWT.WRAP);
		text_1.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		text_1.setEditable(false);
	}

	HWND mouseGetHandle;
	int nHpen;
	DC hdc;
	private Label lbl_Color;

	public void drawBorder() {
		try {
			if (mouseGetHandle != null && mouseGetHandle.getValue().intValue() == handle.getValue().intValue()) {
				return;
			} else {
				if (mouseGetHandle != null && hdc != null) {
					Gdi32.DeleteObject(nHpen);
					User32.ReleaseDC(mouseGetHandle, hdc);
					User32.UpdateWindow(mouseGetHandle);
					System.out.println("User32.ReleaseDC(mouseGetHandle, hdc);User32.UpdateWindow(mouseGetHandle);");
				}
			}
			mouseGetHandle = handle;
			LRECT rect = new LRECT();
			boolean getWindowRect = User32.GetClientRect(handle, rect);
			System.out.println(getWindowRect + "===" + rect.getRight() + "" + rect.getBottom());
			hdc = User32.GetWindowDC(handle);
			int createPen = Gdi32.CreatePen(0, 3, 255);
			Gdi32.SelectObject(hdc, createPen);
			OS.SetROP2(hdc.getValue(), 10);
			rect.setLeft(0);
			rect.setTop(0);
			nHpen = Gdi32.CreatePen(0, 3, 255);
			int nHg = Gdi32.SelectObject(hdc, nHpen);
			Gdi32.Rectangle(hdc, rect.getLeft() + 1, rect.getTop() + 1, rect.getRight() - 1, rect.getBottom() - 1);//
			// 上面的加减是为了更画 窗口矩形的位置准确所运算的
			Gdi32.SelectObject(hdc, nHg);
			// Gdi32.DeleteObject(nHpen);
			// User32.ReleaseDC(handle, hdc);
			// User32.UpdateWindow(handle);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setText(String valueAsString) {
		text_1.append(valueAsString + "\n");
	}

	protected void getHandle(String text2) {
		try {
			handle = User32.FindWindow(null, text2);
			System.out.println(shell.handle + "====" + handle);
			setText("获取窗口:[" + text2 + "]的句柄：" + Integer.toHexString(handle.getValue()).toUpperCase());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void leftClick(int x, int y) {
		try {
			UINT mouseLeftDown = new UINT(0x201);
			UINT mouseLeftUp = new UINT(0x202);
			User32.SendMessage(handle, mouseLeftDown, new WPARAM(0), new LPARAM((y << 16) | x));
			User32.SendMessage(handle, mouseLeftUp, new WPARAM(0), new LPARAM((y << 16) | x));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void getWindowInfo() {
		if (!isFindWindow) {
			return;
		}
		POINT p = new POINT();
		try {
			User32.GetCursorPos(p);
			org.eclipse.swt.internal.win32.POINT lpPoint = new org.eclipse.swt.internal.win32.POINT();
			handle = User32.WindowFromPoint(p);
			OS.ClientToScreen(handle.getValue(), lpPoint);
			int x = p.getX();
			int clientX = x - lpPoint.x;
			int y = p.getY();
			int clientY = y - lpPoint.y;
			if (handle.getValue().intValue() == 0) {
				return;
			}
			String getWindowText = User32.GetWindowText(handle);
			// getWindowText = new String(getWindowText.getBytes("GBK"),"UTF-8");
			Pointer lpClassName = Pointer.createPointer(100);
			int nMaxCount = 100;
			User32.GetClassName(handle, lpClassName, nMaxCount);
			setWindowsInfo(getWindowText, lpClassName);
			drawBorder();
			text_X.setText("" + x);
			text_Y.setText("" + y);
			text_clientX.setText("" + clientX);
			text_clientY.setText("" + clientY);
			getClientColor(clientX, clientY);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * @param getWindowText
	 * @param lpClassName
	 * @throws NativeException
	 */
	public void setWindowsInfo(String getWindowText, Pointer lpClassName) throws NativeException {
		txt_title.setText(getWindowText);
		String hexString = Integer.toHexString(handle.getValue());
		txt_handle.setText(hexString.toUpperCase()+"("+handle.getValue()+")");
		try {
			text_class.setText(lpClassName.getAsString());
		} catch (NativeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取客户端指定点的颜色
	 */
	public void getClientColor(int x, int y) {
		try {
			Image image = new Image(shell.getDisplay(), 5, 5);
			DC hdcSrc = User32.GetDC(new HWND(lbl_Color.handle));
			Gdi32.BitBlt(hdcSrc, 0, 0, 5, 5, User32.GetDC(handle), x, y, 13369376);
			GC gc = new GC(lbl_Color);
			gc.copyArea(image, 1, 1);
			ImageData imageData = image.getImageData();
			int pixel = imageData.getPixel(0, 0);
			PaletteData palette = imageData.palette;
			RGB rgb = palette.getRGB(pixel);
			Color color = new Color(shell.getDisplay(), rgb);
			lbl_Color.setBackground(color);
			String c = Integer.toHexString(color.getRed()) + Integer.toHexString(color.getGreen()) + Integer.toHexString(color.getBlue());
			text_mouseColor.setText(c);
			image.dispose();
			gc.dispose();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
