package com.menage.dllinject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.xvolks.jnative.Convention;
import org.xvolks.jnative.Type;
import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.logging.ConsoleLogger;
import org.xvolks.jnative.logging.JNativeLogger;
import org.xvolks.jnative.logging.JNativeLogger.SEVERITY;
import org.xvolks.jnative.misc.SecurityAttributes;
import org.xvolks.jnative.misc.basicStructures.DWORD;
import org.xvolks.jnative.misc.basicStructures.HANDLE;
import org.xvolks.jnative.misc.basicStructures.HWND;
import org.xvolks.jnative.misc.basicStructures.LONG;
import org.xvolks.jnative.pointers.NullPointer;
import org.xvolks.jnative.pointers.Pointer;
import org.xvolks.jnative.pointers.memory.NativeMemoryBlock;
import org.xvolks.jnative.util.Callback;
import org.xvolks.jnative.util.DbgHelp;
import org.xvolks.jnative.util.Kernel32;
import org.xvolks.jnative.util.StructConverter;
import org.xvolks.jnative.util.WindowProc;
import org.xvolks.jnative.util.ole.Oleaut32;

/**
 * JNative this is the main class for calling native functions.<br>
 *
 * $Id: JNative.java,v 1.72 2008/12/25 21:46:51 thubby Exp $; <br>
 * To do so you have to :
 * <ul>
 * <li>create a new JNative object (JNative messageBox = new
 * JNative("User32.dll", "MessageBoxA");</li>
 * <li>set its return type (messageBox.setRetVal(Type.INT);</li>
 * <li>pass some parameters (messageBox.setParameter(0, Type.INT, "0");</li>
 * <li>pass some parameters (messageBox.setParameter(1, Type.STRING,
 * "message");</li>
 * <li>pass some parameters (messageBox.setParameter(2, Type.STRING,
 * "caption");</li>
 * <li>pass some parameters (messageBox.setParameter(3, Type.INT, "" + 0);</li>
 * <li>then invoke the function (messageBox.invoke();</li>
 * <li>you can get its return value (messageBox.getRetVal();</li>
 * </ul>
 * So simple :) <br>
 * if you have to deal with pointers you can create some, here is a sample, it
 * uses one pointer but could have be done with 3 (one per PULARGE_INTEGER)
 * <hr>
 * The C function to call
 *
 * <pre>
 * BOOL GetDiskFreeSpaceEx(LPCTSTR lpDirectoryName,
 * 		PULARGE_INTEGER lpFreeBytesAvailable,
 * 		PULARGE_INTEGER lpTotalNumberOfBytes,
 * 		PULARGE_INTEGER lpTotalNumberOfFreeBytes);
 *
 * </pre>
 *
 * <HR>
 * The implementation in Java with JNative
 *
 * <pre>
 * public static final FreeDiskSpace getDiskFreeSpaceEx(String drive)
 * 		throws NativeException, IllegalAccessException {
 * 	if (drive == null)
 * 		throw new NullPointerException(&quot;The drive name cannot be null !&quot;);
 * 	Pointer lpFreeBytesAvailable = new Pointer(24);
 * 	int i = 0;
 * 	JNative fs = new JNative(&quot;Kernel32.dll&quot;, &quot;GetDiskFreeSpaceExA&quot;);
 * 	fs.setRetVal(Type.INT);
 * 	fs.setParameter(i++, Type.STRING, drive);
 * 	fs.setParameter(i++, lpFreeBytesAvailable.getPointer(), 8);
 * 	fs.setParameter(i++, lpFreeBytesAvailable.getPointer() + 8, 8);
 * 	fs.setParameter(i++, lpFreeBytesAvailable.getPointer() + 16, 8);
 * 	fs.invoke();
 * 	FreeDiskSpace dsp = new FreeDiskSpace(drive, lpFreeBytesAvailable);
 *  lpFreeBytesAvailable.dispose();
 * 	return dsp;
 * }
 * </pre>
 *
 * <HR>
 * $Id: JNative.java,v 1.72 2008/12/25 21:46:51 thubby Exp $
 *
 * This software is released under the LGPL.
 *
 * @author Created by Marc DENTY - (c) 2006 JNative project
 */
public class MyJNative
{

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    private static final boolean isLinux = System.getProperty("os.name").toLowerCase().indexOf("linux") != -1;
    private static boolean mInitDone;
    private static boolean DEBUG;
    private static JNativeLogger mLogger = null;
    private final static Map<String, MyJNative> mLibs = new HashMap<String, MyJNative>();
    public static final String DLL_NAME = isWindows ? "JNativeCpp.dll" : "libJNativeCpp.so";
    private static int currentHModule;
    /**
     * Pointer on the function address
     */
    private int mJNativePointer;
    private boolean mIsClosed = false;
    private final String mDllName;
    private final String mFunctionName;
    private static Map<Integer, Callback> mCallbacks = new TreeMap<Integer, Callback>();
    /*
     * The following fields are used by native side directly and must not be renamed!!
     */
    /**
     * Pre-allocated parameter array, need to grow if needed
     */
    private Vector<byte[]> parameters = new Vector<byte[]>();
    private Vector<Integer> parameterTypes = new Vector<Integer>();
    private String mRetValue;
    // Used by native side (handle of the library)
    private int mJNativeHModule;
    // Used by native side
    private int convention;
    // Used by native side
    private int lastError;

    @SuppressWarnings("unused")
    private int mRetType;

    @SuppressWarnings("unused")
    private Vector<byte[]> getParameters()
    {
        return parameters;
    }

    @SuppressWarnings("unused")
    private Vector<Integer> getParameterTypes()
    {
        return parameterTypes;
    }

    private native int nLoadLibrary(String dllName, String funcPointer) throws NativeException;

    public native int nFindFunction(int libHandle, String funcPointer) throws NativeException;

    // private native void nSetParameter(int jNativePointer, int pos, String
    // type,
    // byte[] value) throws NativeException;
//	private native void nSetPointer(int jNativePointer, int pos, int pointer,
//			int size) throws NativeException;
//
    private native String nGetParameter(int jNativePointer, int pos)
            throws NativeException;

    private native void nInvoke(int jNativePointer) throws NativeException;

    private native void nDispose(int jNativePointer) throws NativeException;

    private static native int nMalloc(int size) throws NativeException;

    private static native void nFree(int pointer) throws NativeException;

    private static native void nSetMemory(int pointer, byte[] buff, int offset,
            int len) throws NativeException;

    private static native byte[] nGetMemory(int pointer, int len)
            throws NativeException;

    private static native int nRegisterWindowProc(int hwnd, Object winProc,
            boolean custom) throws NativeException;

    private static native int nGetCurrentModule() throws NativeException;

    private static native int nCreateCallBack(int numParams)
            throws NativeException;

    private static native boolean nReleaseCallBack(int pos)
            throws NativeException;

    private static native int nGetNativePattern(int jNativePointer,
            byte[] pattern, int maxLen) throws NativeException;

    private static native String nGetNativeSideVersion() throws NativeException;

    private static void loadFromJar() throws UnsatisfiedLinkError
    {
        File tempDir = new File(System.getProperty("user.dir"));
        File dllFile = new File(tempDir, DLL_NAME);
        // Thubby: This returns null for me!
        //InputStream in = JNative.class.getResourceAsStream("../../../lib-bin/" + DLL_NAME);
        InputStream in = MyJNative.class.getResourceAsStream("/lib-bin/" + DLL_NAME);
        if (in == null)
        {
            if (!dllFile.exists())
            {
                throw new UnsatisfiedLinkError(DLL_NAME + " : unable to find in " + tempDir);
            }
        }
        else
        {
            if (dllFile.exists() && dllFile.canWrite())
            {
                dllFile.delete();
            }
            if (!dllFile.exists())
            {
                byte[] buffer = new byte[512];
                BufferedOutputStream out = null;
                try
                {
                    try
                    {
                        out = new BufferedOutputStream(new FileOutputStream(dllFile));
                        while (true)
                        {
                            int readed = in.read(buffer);
                            if (readed > -1)
                            {
                                out.write(buffer, 0, readed);
                            }
                            else
                            {
                                break;
                            }
                        }
                    }
                    finally
                    {
                        if (out != null)
                        {
                            out.close();
                        }
                    }
                }
                catch (IOException e)
                {
                    throw new UnsatisfiedLinkError("Can't write library in " + dllFile);
                }
            }
            System.load(dllFile.toString());
        }
    }


    static
    {
        boolean lInit = false;

        String debug = System.getProperty("jnative.debug");
        if (debug != null)
        {
            try
            {
                setLoggingEnabled(Boolean.parseBoolean(debug));
            }
            catch (Exception e)
            {
                System.err.println("DEBUG messages disabled!");
                e.printStackTrace();
                setLoggingEnabled(false);
            }
        }
        else
        {
            setLoggingEnabled(false);
        }

        //setLoggingEnabled(true);

        // Thubby: as we are now storing the first JNative instance of each DLL we should not use WeakHashMap!
        /*
        // Manage the type of the Map used
        mLibs = Boolean.parseBoolean(""+System.getProperty("jnative.weakRef"))
        ?
        new WeakHashMap<String, JNative>()
        :
        new HashMap<String, JNative>();
        getLogger().log(SEVERITY.DEBUG, "Using a "+mLibs.getClass().getName() + " as native handles storage");
         */

        try
        {
            String loadNative = System.getProperty("jnative.loadNative");
            getLogger().log(SEVERITY.DEBUG, "jnative.loadNative property = " + loadNative);
            if (loadNative == null || loadNative.equalsIgnoreCase("default"))
            {
                getLogger().log(SEVERITY.DEBUG, "Using default System.loadLibrary()");
                try
                {
                    System.loadLibrary("JNativeCpp");
                }
                catch (UnsatisfiedLinkError e)
                {
                    getLogger().log(SEVERITY.INFO, "Library not found, trying to extract it from JAR");
                    loadFromJar();
                }
                lInit = true;
            }
            else if (loadNative.equalsIgnoreCase("manual"))
            {
                getLogger().log(SEVERITY.DEBUG, "Using manual : you MUST load the library yourself, then init callbacks !");
                lInit = true;
            }
            else
            {
                getLogger().log(SEVERITY.DEBUG, "Trying to load Library from " + loadNative);
                System.load(loadNative);
                lInit = true;
            }
        }
        catch (Throwable e)
        {
            getLogger().log(SEVERITY.ERROR, e);
        }
        finally
        {
            mInitDone = lInit;
            boolean ok = false;
            String nativeSideVersion = null;
            try
            {
                nativeSideVersion = getNativeSideVersion();
                //nativeSideVersion = "1.3.1";
                for (String s : getCompatibleNativeVersion())
                {
                    if (nativeSideVersion.equals(s))
                    {
                        ok = true;
                        break;
                    }
                }
            }
            catch (Throwable e)
            {
                getLogger().log(SEVERITY.ERROR, e);
            }
            if (!ok)
            {
                getLogger().log(SEVERITY.WARN, "Caution : the native side version (" + nativeSideVersion + ") is not in the compatibility list.");

            }

            // add a shutdown hook that automatically unloads all libraries on JVM exit.
            Runtime.getRuntime().addShutdownHook(new Thread()
            {

                public void run()
                {
                    unLoadAllLibraries();
                }
            });

        }
    }

    public static int callback(int address, long[] values)
    {
        getLogger().log(SEVERITY.DEBUG, String.format("in Java callback #%x with %d arguments\n", address, values.length));
        final Callback c = mCallbacks.get(address);
        if (c != null)
        {
            return c.callback(values);
        }
        return -1;
    }

    @Override
    public String toString()
    {
        return mDllName + "-" + mFunctionName;
    }

    public static void setDefaultCallingConvention(Convention defaultConvention)
    {
//        Convention.setDefaultStyle(defaultConvention);
    }

    /**
     * Creates a function without debug output that can call an anonymous function by it's address
     * 	 * @exception NativeException
     *                if the dll was not found, function name is incorrect...
     */
    public MyJNative(int address,String functionName, Convention convention) throws NativeException
    {
        if (!mInitDone)
        {
            throw new IllegalStateException(
                    "JNative library not loaded, sorry !");
        }
        this.convention = convention.getValue();
        mDllName = "Anonymous";
        mFunctionName = mDllName;
        mJNativePointer = address;
        try
        {
            setRetVal(Type.VOID);
        }
        catch (IllegalAccessException e)
        {
            getLogger().log(SEVERITY.ERROR, e);
        }
    }

    /**
     * Constructor exact call of new JNative(dllName, functionName, false, Convention.DEFAULT); <br>
     * Creates a function without debug output
     *
     * @param dllName
     *            the name of library file
     * @param functionName
     *            the decorated name of the function (MessageBoxA instead of
     *            MessageBox)
     *
     * @exception NativeException
     *                if the dll was not found, function name is incorrect...
     * @see org.xvolks.jnative.JNative#getDLLFileExports(String)
     */
    public MyJNative(String dllName, String functionName) throws NativeException
    {
        this(dllName, functionName, false, Convention.DEFAULT);
    }

    /**
     * Constructor exact call of new JNative(dllName, functionName, false, convention); <br>
     * Creates a function without debug output
     *
     *
     *
     * @param dllName
     *            the name of library file
     * @param functionName
     *            the decorated name of the function (MessageBoxA instead of
     *            MessageBox)
     * @param convention
     * 			  convention of function call
     * @exception NativeException
     *                if the dll was not found, function name is incorrect...
     * @see org.xvolks.jnative.JNative#getDLLFileExports(String)
     */
    public MyJNative(String dllName, String functionName, Convention convention) throws NativeException
    {
        this(dllName, functionName, false, convention);
    }

    /**
     * Constructor exact call of new JNative(dllName, functionName, debug, Convention.DEFAULT); <br>
     * Creates a function without debug output
     *
     * @deprecated the debug parameter in meaningless now
     * @param dllName
     *            the name of library file
     * @param functionName
     *            the decorated name of the function (MessageBoxA instead of
     *            MessageBox)
     * @param nativeDebug
     *            a boolean if true the dll logs output on stdout (beware this
     *            is shared between all instances of JNative)
     *
     * @exception NativeException
     *                if the dll was not found, function name is incorrect...
     * @see org.xvolks.jnative.JNative#getDLLFileExports(String)
     */
    public MyJNative(String dllName, String functionName, boolean debug) throws NativeException
    {
        this(dllName, functionName, debug, Convention.DEFAULT);
    }

    /**
     * Constructor
     *
     *
     *
     * @deprecated the nativeDebug in meaningless now
     * 
     * @param dllName
     *            a String the name of the library; that DLL must be in the
     *            library.path
     * @param functionName
     *            a String the name of the function this is the decorated name
     *            (@see org.xvolks.jnative.JNative#getDLLFileExports(String))
     * @param nativeDebug
     *            a boolean if true the dll logs output on stdout (beware this
     *            is shared between all instances of JNative)
     * @param convention
     * 			  convention of function call
     * @exception NativeException
     *                if the dll was not found, function name is incorrect...
     * @see org.xvolks.jnative.JNative#getDLLFileExports(String)
     */
    public MyJNative(String dllName, String functionName, boolean nativeDebug, Convention convention)
            throws NativeException
    {
        if (!mInitDone)
        {
            throw new IllegalStateException(
                    "JNative library not loaded, sorry !");
        }
        this.convention = convention.getValue();
        mDllName = dllName;
        mFunctionName = functionName;

        // load the library
        loadLibrary();

        try
        {
            setRetVal(Type.VOID);
        }
        catch (IllegalAccessException e)
        {
            getLogger().log(SEVERITY.ERROR, e);
        }
    }

    // Loads the native library
    private final boolean loadLibrary() throws NativeException
    {
        synchronized (mLibs)
        {
            // search the cache for this library
            final MyJNative libDesc = mLibs.get(mDllName);

            // has this library been loaded already?
            if (libDesc == null || libDesc.getHModule() == 0)
            {
                // not yet loaded --> do it now
                mJNativePointer = nLoadLibrary(mDllName, mFunctionName);
                // add the reference to the cache
                mLibs.put(mDllName, this);
                getLogger().log(SEVERITY.DEBUG, "Successfully loaded library '" + getDLLName() + "', functionName = " + getFunctionName() + ": hModule = " + getHModule());
            }
            // library has been loaded already
            else
            {
                // get the hModule
                mJNativeHModule = libDesc.getHModule();
                // find the function in the already loaded library
                mJNativePointer = nFindFunction(mJNativeHModule, mFunctionName);
                getLogger().log(SEVERITY.TRACE, "Reusing cached handle " + libDesc.getHModule() + " for function '" + getFunctionName() + "' in library '" + libDesc.getDLLName() + "'");
            }
            mIsClosed = false;
            return mJNativePointer != 0;
        }
    }

    public static final boolean isLibraryLoaded(String name)
    {
        synchronized (mLibs)
        {
            final MyJNative libDesc = mLibs.get(name);

            return (libDesc != null && libDesc.getHModule() != 0 && libDesc.getFunctionPointer() != 0);
        }
    }

    /**
     * Gets the native pointer of a function, can be used to pass function pointer to an other function.
     * @return the native function pointer
     */
    public int getFunctionPointer()
    {
//        throwClosed();
        return mJNativePointer;
    }

    /**
     * Gets the native handle to the dll referenced by this JNative instance
     * @return the HMODULE associated with the native DLL referenced by this JNative instance
     */
    public int getHModule()
    {
        return mJNativeHModule;
    }

    public void setParameter(int pos, int value) throws /* NativeException, */
            IllegalAccessException
    {
        setParameter(pos, Type.INT, value + "");
    }

    /**
     * Method setParameter <br>
     * Sets the parameter at index <code>pos</code>
     *
     * @param pos
     *            the offset of the parameter
     * @param type
     *            one of the enum entry (INT, STRING...)
     * @param value
     *            the parameter in its String representation
     *
     * @throws IllegalAccessException
     *             if this object has been disposed
     *
     */
    public void setParameter(int pos, Type type, String value)
            throws IllegalAccessException
    {
        if (value == null)
        {
            setParameter(pos, 0);
        }
        else
        {
            setParameter(pos, type, (value + '\0').getBytes());
        }
    }

    /**
     * Method setParameter <br>
     * Sets the parameter at index <code>pos</code>
     *
     * @param pos
     *            the offset of the parameter
     * @param value
     *            the String parameter (this parameter must be a in one) !
     * @throws IllegalAccessException
     *             if this object has been disposed
     *
     */
    public void setParameter(int pos, String lValue)
            throws IllegalAccessException
    {
        setParameter(pos, Type.STRING, lValue);
    }

    /**
     * GetLastError workaround for Win32.
     * 
     * @return the GetLastError status set by the last call to nInvoke 
     */
    public int getLastError() {
    	return lastError;
    }
    
    /**
     * Method setParameter <br>
     * Sets the parameter at index <code>pos</code>
     *
     * @param pos
     *            the offset of the parameter
     * @param type
     *            one of the enum entry (INT, STRING...)
     * @param value
     *            the parameter in its byte[] representation
     *
     * @exception NativeException
     *
     */
    public void setParameter(int pos, Type type, byte[] value)
            throws /* NativeException, */ IllegalAccessException
    {
        //throwClosed();

        if (parameters.size() <= pos)
        {
            // Fills the vector with null and expect that the call will give
            // right values later
            int i = parameters.size();
            while (i++ <= pos)
            {
                parameters.add(new byte[4]);
                parameterTypes.add(Type.INT.getNativeType());
            }
        }
        parameters.set(pos, value);
        parameterTypes.set(pos, type.getNativeType());

    // ---------------------------------------DEBUG CODE -
    // final String pouet;
    // if(parameterTypes.get(pos) == Type.PSTRUCT.getNativeType()) {
    // int val=StructConverter.bytesIntoInt(parameters.get(pos), 0);
    // pouet = "0x"+Integer.toHexString(val)+ " soit "+val+" dec";
    // } else {
    // pouet = new String(parameters.get(pos));
    // }
    // getLogger().log(SEVERITY.DEBUG, "Adding parameter "+pos+", value = "+pouet);
    // ---------------------------------------DEBUG CODE -

    }

    /**
     * Method setParameter <br>
     * Sets the parameter at index <code>pos</code>
     *
     * @param pos
     *            the offset of the parameter
     * @param p
     *            a pointer object representing an address
     *
     * @exception NativeException
     * @exception IllegalAccessException
     *                if <code>dispose()</code> have already been called.
     *
     */
    public void setParameter(int pos, Pointer p) throws NativeException,
            IllegalAccessException
    {
        //throwClosed();
        if (p == null || p.getPointer() == 0)
        {
            setParameter(pos, 0);
        }
        else
        {
            byte[] buf = new byte[4];
            StructConverter.intIntoBytes(p.getPointer(), buf, 0);
            setParameter(pos, Type.PSTRUCT, buf);
        // nSetPointer(mJNativePointer, pos, p.getPointer(), p.getSize());
        }
    }

    /**
     * Method setRetVal fixes the return type of this function.
     *
     * @param type
     *            a Type generally VOID or INT, if VOID it is not necessary to
     *            call this function
     *
     * @exception NativeException
     * @exception IllegalAccessException
     *                if <code>dispose()</code> have already been called.
     *
     */
    public void setRetVal(Type type) throws NativeException,
            IllegalAccessException
    {
        //throwClosed();
        mRetType = type.getNativeType();
    }

    /**
     * Method getRetVal gets the value returned by the function, should be
     * verified to avoid invalid pointers when getting out values
     *
     * @return the value returned by this function in its String representation<br>
     *         BOOL functions return int values
     *
     * @exception IllegalAccessException
     *                if <code>dispose()</code> have already been called.
     *
     */
    public String getRetVal() throws IllegalAccessException
    {
        throwClosed();
        return mRetValue;
    }

    /**
     * Method getRetValAsInt gets the value returned by the function, should be
     * verified to avoid invalid pointers when getting out values
     *
     * @return the value returned by this function in its Integer representation<br>
     *         BOOL functions return int values
     *
     * @exception IllegalAccessException
     *                if <code>dispose()</code> have already been called.
     *
     */
    public int getRetValAsInt() throws IllegalAccessException
    {
        return new Long(getRetVal()).intValue();
    }

    /**
     * Method getParameter
     *
     * @param pos
     *            an int
     *
     * @return the parameter at index <code>pos</code>
     *
     * @exception NativeException
     * @exception IllegalAccessException
     *                if <code>dispose()</code> have already been called.
     *
     */
    public String getParameter(int pos) throws NativeException, IllegalAccessException
    {
        throwClosed();
        return nGetParameter(mJNativePointer, pos);
    }

    /**
     * Method invoke calls the function
     *
     * @exception NativeException
     * @exception IllegalAccessException
     *                if <code>dispose()</code> have already been called.
     *
     */
    public void invoke() throws NativeException, IllegalAccessException
    {
        //throwClosed();

        // check if the library was (accidentially) unloaded before invoke() was called
        if (!isLibraryLoaded(mDllName) && !"Anonymous".equals(mDllName))
        {
            getLogger().log("Library '" + mDllName + "' is currently not loaded! Loading it now...");
            loadLibrary();
        }
        nInvoke(mJNativePointer);
    }

    /**
     * Method disposes free native pointers and memory internally used by the
     * jnative dll
     * <p style="text-align: center; font-weight: bolder;">
     * >>>>>>> Should not be called manually! <<<<<<<
     * </p>
     * @exception NativeException
     *
     */
    private synchronized final void unLoad() throws NativeException
    {
        //throwClosed();

        try
        {
            if (isLibraryLoaded(mDllName))
            {
                getLogger().log(SEVERITY.DEBUG, "Unloading native library '" + mDllName + "'");
                nDispose(mJNativeHModule);
            }
            mIsClosed = true;
            mJNativeHModule = 0;
        }
        finally
        {
            synchronized (mLibs)
            {
                mLibs.remove(mDllName);
            }
        }
    }

    /**
     *  Unloads a specific library. Only call this if you really know what you are doing!
     * @author Thubby - 4 juil. 2008
     * @param name the name of the library exactly spelled like at creation time.
     * @return true if the library was freed
     * @throws NativeException
     */
    public static final boolean unLoadLibrary(String name) throws NativeException
    {
        synchronized (mLibs)
        {
            if (mLibs.containsKey(name))
            {
                mLibs.get(name).unLoad();
                return true;
            }
            return false;
        }
    }

    /**
     * Unloads all loaded libraries. This is automatically called when the JVM exits but may also be called to clean up.
     * Only call this if you really know what you are doing!
     *
     * @author Thubby (mdt) - 4 juil. 2008
     */
    public static final void unLoadAllLibraries()
    {
        synchronized (mLibs)
        {
            if (!mLibs.isEmpty())
            {
                // sleep a little
                try
                {
                    Thread.sleep(100L);
                }
                catch (InterruptedException ex)
                {
                    // ignore
                }

                Object keys[] = mLibs.keySet().toArray();
                for (int i = 0; i < keys.length; i++)
                {
                    try
                    {
                        unLoadLibrary(keys[i].toString());
                    }
                    catch (Throwable e)
                    {
                        getLogger().log(SEVERITY.WARN, "Error while unloading library '" + keys[i].toString() + "': " + e.toString());
                    }
                }
            }
        }
    }

    /**
     * Called by the garbage collector on an object when garbage collection
     * determines that there are no more references to the object. A subclass
     * overrides the <code>finalize</code> method to dispose of system
     * resources or to perform other cleanup.
     * <p>
     * The general contract of <tt>finalize</tt> is that it is invoked if and
     * when the Java<font size="-2"><sup>TM</sup></font> virtual machine has
     * determined that there is no longer any means by which this object can be
     * accessed by any thread that has not yet died, except as a result of an
     * action taken by the finalization of some other object or class which is
     * ready to be finalized. The <tt>finalize</tt> method may take any
     * action, including making this object available again to other threads;
     * the usual purpose of <tt>finalize</tt>, however, is to perform cleanup
     * actions before the object is irrevocably discarded. For example, the
     * finalize method for an object that represents an input/output connection
     * might perform explicit I/O transactions to break the connection before
     * the object is permanently discarded.
     * <p>
     * The <tt>finalize</tt> method of class <tt>Object</tt> performs no
     * special action; it simply returns normally. Subclasses of <tt>Object</tt>
     * may override this definition.
     * <p>
     * The Java programming language does not guarantee which thread will invoke
     * the <tt>finalize</tt> method for any given object. It is guaranteed,
     * however, that the thread that invokes finalize will not be holding any
     * user-visible synchronization locks when finalize is invoked. If an
     * uncaught exception is thrown by the finalize method, the exception is
     * ignored and finalization of that object terminates.
     * <p>
     * After the <tt>finalize</tt> method has been invoked for an object, no
     * further action is taken until the Java virtual machine has again
     * determined that there is no longer any means by which this object can be
     * accessed by any thread that has not yet died, including possible actions
     * by other objects or classes which are ready to be finalized, at which
     * point the object may be discarded.
     * <p>
     * The <tt>finalize</tt> method is never invoked more than once by a Java
     * virtual machine for any given object.
     * <p>
     * Any exception thrown by the <code>finalize</code> method causes the
     * finalization of this object to be halted, but is otherwise ignored.
     *
     *
     * @throws Throwable
     *             the <code>Exception</code> raised by this method
     */
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    /*
    try
    {
    dispose();
    }
    catch (Throwable e)
    {
    // getLogger().log(SEVERITY.ERROR, e);
    }
     */
    }

    /**
     * Method getFunctionName
     *
     * @return the name of this function
     *
     */
    public final String getFunctionName()
    {
        return mFunctionName;
    }

    /**
     * Method getDLLName
     *
     * @return the name of this DLL
     *
     */
    public final String getDLLName()
    {
        return mDllName;
    }

    /**
     *
     *
     *
     * @return the convention of call (CDECL, STDCALL)
     */
    public Convention getStyle()
    {
        return Convention.fromInt(convention);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @returns true if the dll and the funtionName are equals
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof MyJNative))
        {
            return false;
        }
        else
        {
            MyJNative n = (MyJNative) obj;
            return mDllName.equals(n.getDLLName()) && mFunctionName.equals(n.getFunctionName());
        }
    }

    private void throwClosed() throws IllegalAccessException
    {
        if (mIsClosed)
        {
            throw new IllegalAccessException("This function (" + getFunctionName() + " in " + getDLLName() + ") is already closed");
        }
    }

    /**
     * Method allocMemory allocates native block of memory : don't forget to
     * free it with <code>freeMemory(int)</code>!!!!
     *
     * @param size
     *            the size of the memory block to reserve in bytes
     *
     * @return the address of the reserved memory (a pointer)
     *
     * @exception NativeException
     *                if the memory cannot be reserved
     *
     */
    public static int allocMemory(int size) throws NativeException
    {
        return nMalloc(size);
    }

    /**
     * Method freeMemory try to free the block of memory pointed by
     * <code>pointer</code><br>
     * No checks are done to see if the pointer is valid (TODO ?)
     *
     * @param pointer
     *            the address of a memory block to free
     *
     * @exception NativeException
     *                if the memory cannon be freed
     *
     */
    public static void freeMemory(int pointer) throws NativeException
    {
        nFree(pointer);
    }

    /**
     * Method setMemory fills the native memory with the content of
     * <code>buffer</code> <br>
     * <b>Be aware that buffer overflows are no checked !!!!</b>
     *
     * @param pointer
     *            the address of the native memory
     * @param buffer
     *            a String to memcpy
     *
     * @exception NativeException
     *
     */
    public static void setMemory(int pointer, String buffer)
            throws NativeException
    {
        setMemory(pointer, buffer.getBytes());
    }

    /**
     * Method setMemory fills the native memory with the content of
     * <code>buffer</code> <br>
     * <b>Be aware that buffer overflows are no checked !!!!</b>
     *
     * @param pointer
     *            the address of the native memory
     * @param buffer
     *            a byte[] to memcpy
     *
     * @exception NativeException
     *
     */
    public static void setMemory(int pointer, byte[] buffer)
            throws NativeException
    {
        setMemory(pointer, buffer, 0, buffer.length);
    }

    /**
     * Method setMemory fills the native memory with the content of
     * <code>buffer</code> <br>
     * <b>Be aware that buffer overflows are no checked !!!!</b>
     *
     * @param pointer
     *            the address of the native memory
     * @param buffer
     *            a byte[] to memcpy
     * @param offset
     *            the offset of the native side
     * @param len
     *            the number of bytes to copy
     *
     * @exception NativeException
     *
     */
    public static void setMemory(int pointer, byte[] buffer, int offset, int len)
            throws NativeException
    {
        nSetMemory(pointer, buffer, offset, len);
    }

    /**
     * Method getMemory
     *
     * @param pointer
     *            the address of the native memory
     * @param size
     *            number of bytes to copy
     *
     * @return a copy of the memory at address <code>pointer</code>
     *
     * @exception NativeException
     *
     */
    public static byte[] getMemory(int pointer, int size)
            throws NativeException
    {
        return nGetMemory(pointer, size);
    }

    // returns the length of the string the pointer is pointing to
    // might cause high CPU-usage for very large Strings or when used repeatedly in a loop
    public static int getStrLen(int pointer) throws NativeException
    {
        int counter = 0;
        while (getMemory(pointer++, 1)[0] != 0)
        {
            counter++;
        }
        return counter;
    }

    /**
     * Method getMemoryAsString
     *
     * @param pointer
     *            the address of the native char*
     *
     * @return a String copy of the memory at address <code>pointer</code> to the next null-terminator (best-effort).
     *          More CPU-intensive than getMemoryAsString(int pointer, int size) as we are grabbing the String byte-by-byte and check each byte for null-termination
     *
     *
     * @exception NativeException
     *
     */
    public static String getMemoryAsString(int pointer) throws NativeException
    {
        return getMemoryAsString(pointer, getStrLen(pointer));
    }

    /**
     * Method getUnicodeMemoryAsString
     *
     * @param pointer
     *            the address of the native char*
     *
     * @return a String copy of the memory at address <code>pointer</code> to the next null-terminator (best-effort).
     *          If the Pointer contains a Unicode String use this instead of getMemoryAsString!
     *
     *
     * @exception NativeException
     *
     */    
    public static String getUnicodeMemoryAsString(int pointer) throws NativeException
    {
        StringBuffer s = new StringBuffer();
        byte[] b;
        while ((b = getMemory(pointer, 1))[0] != 0)
        {
            s.append(new String(b));
            pointer+=2;
        }
        return s.toString();
    }

    /**
     * Method getMemoryAsString
     *
     * @param pointer
     *            the address of the native char*
     * @param size
     *            number of bytes to copy
     *
     * @return a String copy of the memory at address <code>pointer</code>
     *         limited by a NULL terminator if the char* is lower than </code>size</code>
     *         characters
     *
     * @exception NativeException
     *
     */
    public static String getMemoryAsString(int pointer, int size) throws NativeException
    {
        byte[] buf = nGetMemory(pointer, size);

        for (int i = 0; i < buf.length; i++)
        {
            if (buf[i] == 0)
            {
                return new String(buf, 0, i);
            }
        }
        return new String(buf);
    }

    // reads the Memory completely without stopping at the first NULL-Terminator
    public static String getMemoryAsString(int pointer, int size, boolean readFully) throws NativeException
    {
        if(readFully)
        {
            return new String(nGetMemory(pointer, size));
        }
        return getMemoryAsString(pointer, size);
    }

    /**
     * Method registerWindowProc register a WindowProc for the Window
     * <code>hwnd</code>
     *
     * @param hwnd
     *            the HANDLE of the window
     * @param proc
     *            a WindowProc object that be called by native events
     *
     * @return the previous function pointer used as a WindowProc for this
     *         window.
     *
     * @exception NativeException
     *                if the SetWindowLongPtr fails or somthing weird happens
     *                (can crash too ;) )...
     *
     */
    public static int registerWindowProc(int hwnd, WindowProc proc) throws NativeException
    {
        return nRegisterWindowProc(hwnd, proc, false);
    }

    /**
     * Method registerWindowProc register a WindowProc for the Window.<br>Calling this method is equivalent to call
     * <br><i>registerWindowProc(hwnd.getValue(), proc)</i><br>
     *
     * @param hwnd
     *            the HANDLE of the window
     * @param proc
     *            a WindowProc object that be called by native events
     *
     * @return the previous function pointer used as a WindowProc for this
     *         window.
     *
     * @exception NativeException
     *                if the SetWindowLongPtr fails or somthing weird happens
     *                (can crash too ;) )...
     *
     */
    public static int registerWindowProc(HWND hwnd, WindowProc proc) throws NativeException
    {
        return nRegisterWindowProc(hwnd.getValue(), proc, false);
    }

    /**
     * Method createCallback when a callback is no more used you should/must
     * release it with <code>releaseCallback</code>
     *
     * @param numParams
     *            the number of parameters the callback function sould receive
     * @param callback
     *            a Callback object that will handle the callback
     *
     * @return the native handle of the callback function (this is function
     *         pointer)
     *
     * @exception NativeException
     *                if something goes wrong
     *
     * @version 5/20/2006
     */
    public static int createCallback(int numParams, Callback callback)
            throws NativeException
    {
        Integer address = nCreateCallBack(numParams);
        getLogger().log(SEVERITY.DEBUG, String.format("registering callback %x\n", address));

        mCallbacks.put(address, callback);
        return address;
    }

    /**
     * Method releaseCallback releases a callback previously created with
     * <code>createCallback</code>
     *
     * @param callback
     *            a Callback
     *
     * @return true is this callback was released sucessfully
     *
     * @exception NativeException
     *
     */
    public static boolean releaseCallback(Callback callback)
            throws NativeException
    {
        boolean ret = false;
        if (null != mCallbacks.remove(callback.getCallbackAddress()))
        {
            ret = nReleaseCallBack(callback.getCallbackAddress());
            getLogger().log(SEVERITY.DEBUG, String.format("released callback %x\n", callback.getCallbackAddress()));
        }
        return ret;
    }

    /**
     * Method getCurrentModule
     *
     * @return the HMODULE associated with the jnative DLL
     *
     * @exception NativeException
     *
     */
    public static int getCurrentModule() throws NativeException
    {
        if (currentHModule == 0)
        {
            currentHModule = nGetCurrentModule();
        }
        // check hModule
        if (currentHModule == 0)
        {
            MyJNative.getLogger().log("JNative.nGetCurrentModule() returns 0, seems we still do not get the correct native JNative-library handle... ;-(");
            try
            {
                currentHModule = Kernel32.LoadLibrary(MyJNative.DLL_NAME).getValue();
            }
            catch (IllegalAccessException ex)
            {
                ex.printStackTrace();
            }
        }
        return currentHModule;
    }
    // returns if a function is exported in the given library

    public static boolean isFunctionExported(String lib, String function) throws NativeException, InterruptedException
    {
        String[] funcs = getDLLFileExports(lib);
        if (funcs != null)
        {
            for (int i = 0; i < funcs.length; i++)
            {
                if (funcs[i].equalsIgnoreCase(function))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method getDLLFileExports gets all the names of the functions exported by
     * a library
     *
     * @param dllFile
     *            the name of a library
     * @param demangled
     *            if true JNative tries to demangle C++ function names
     *
     * @return a String[] the names of the functions
     *
     * @exception NativeException
     * @exception IllegalAccessException
     *
     */
    public static String[] getDLLFileExports(String dllFile, boolean demangled)
            throws NativeException, InterruptedException
    {
        if (isWindows)
        {
            try
            {
                HANDLE hFile = Kernel32.CreateFile(dllFile,
                        Kernel32.AccessMask.GENERIC_READ,
                        Kernel32.ShareMode.FILE_SHARE_READ, null,
                        Kernel32.CreationDisposition.OPEN_EXISTING,
                        Kernel32.FileAttribute.FILE_ATTRIBUTE_NORMAL, 0);
                if (hFile.equals(HANDLE.INVALID_HANDLE_VALUE))
                {
                    getLogger().log(SEVERITY.DEBUG,
                            String.format(
                            ">>>ERROR<<< : %s file not found, CreateFile returned an invalid handle\n",
                            dllFile));
                    return null;
                }

                HANDLE hFileMapping = Kernel32.CreateFileMapping(hFile,
                        (SecurityAttributes) null,
                        Kernel32.PageAccess.PAGE_READONLY, new DWORD(0),
                        new DWORD(0), (String) null);
                if (hFileMapping.equals(HANDLE.INVALID_HANDLE_VALUE))
                {
                    Kernel32.CloseHandle(hFile);
                    getLogger().log(SEVERITY.DEBUG,
                            String.format(">>>ERROR<<< : CreateFileMapping returned a NULL handle\n"));
                    return null;
                }

                LONG lpFileBase = Kernel32.MapViewOfFileEx(hFileMapping,
                        Kernel32.FileMap.FILE_MAP_READ, new DWORD(0),
                        new DWORD(0), new DWORD(0), new LONG(0));
                if (lpFileBase.getValue() == 0)
                {
                    Kernel32.CloseHandle(hFileMapping);
                    Kernel32.CloseHandle(hFile);
                    getLogger().log(SEVERITY.DEBUG,
                            String.format(">>>ERROR<<< : MapViewOfFile returned 0\n"));
                    return null;
                }
                int IMAGE_DOS_HEADER_SIZE = 14 * 2 + 4 * 2 + 2 * 2 + 10 * 2 + 4;
                // Composed of Signature + IMAGE_FILE_HEADER +
                // IMAGE_OPTIONAL_HEADER32
                // See
                // http://www.reactos.org/generated/doxygen/d1/d72/struct__IMAGE__OPTIONAL__HEADER32.html
                // for example
                // typedef struct _IMAGE_NT_HEADERS32 {
                // DWORD Signature;
                // IMAGE_FILE_HEADER FileHeader;
                // IMAGE_OPTIONAL_HEADER32 OptionalHeader;
                // }
                int IMAGE_NUMBEROF_DIRECTORY_ENTRIES = 16;
//				int IMAGE_DIRECTORY_ENTRY_EXPORT = 0;
                int IMAGE_OPTIONAL_HEADER32_SIZE = 24 * 4 + IMAGE_NUMBEROF_DIRECTORY_ENTRIES * 2 * 4;
                int IMAGE_NT_HEADERS32_SIZE = 4 + 5 * 4 + IMAGE_OPTIONAL_HEADER32_SIZE;
                int IMAGE_NT_SIGNATURE = 0x00004550;
                int IMAGE_EXPORT_DIRECTORY_SIZE = 10 * 4;
                Pointer pImg_DOS_Header = new Pointer(new NativeMemoryBlock(
                        lpFileBase.getValue(), IMAGE_DOS_HEADER_SIZE));

                Pointer pImg_NT_Header = new Pointer(new NativeMemoryBlock(
                        pImg_DOS_Header.getPointer() + pImg_DOS_Header.getAsInt(IMAGE_DOS_HEADER_SIZE - 4),
                        IMAGE_NT_HEADERS32_SIZE));

                if (!Kernel32.IsBadReadPtr(pImg_NT_Header) || pImg_NT_Header.getAsInt(0) != IMAGE_NT_SIGNATURE)
                {
                    Kernel32.UnmapViewOfFile(lpFileBase);
                    Kernel32.CloseHandle(hFileMapping);
                    Kernel32.CloseHandle(hFile);
                    getLogger().log(SEVERITY.DEBUG,
                            String.format(
                            ">>>ERROR<<< : IsBadReadPtr returned false, pointer is %d\n",
                            pImg_NT_Header.getPointer()));
                    return null;
                }
                getLogger().log(SEVERITY.DEBUG,
                        String.format(
                        ">>>INFO<<< : IsBadReadPtr returned true, pointer is %d\n",
                        pImg_NT_Header.getPointer()));
                // IMAGE_OPTIONAL_HEADER32
                Pointer pOptionalHeader = new Pointer(new NativeMemoryBlock(
                        pImg_NT_Header.getPointer() + IMAGE_NT_HEADERS32_SIZE - IMAGE_OPTIONAL_HEADER32_SIZE,
                        IMAGE_OPTIONAL_HEADER32_SIZE));
                // (IMAGE_EXPORT_DIRECTORY)

                Pointer pImg_Export_Dir = new Pointer(new NativeMemoryBlock(
                        pOptionalHeader.getAsInt(24 * 4), 4));
                if (pImg_Export_Dir.isNull())
                {
                    Kernel32.UnmapViewOfFile(lpFileBase);
                    Kernel32.CloseHandle(hFileMapping);
                    Kernel32.CloseHandle(hFile);
                    getLogger().log(SEVERITY.DEBUG,
                            String.format(">>>ERROR<<< : pImg_Export_Dir is NULL\n"));
                    return null;
                }
                pImg_Export_Dir = new Pointer(new NativeMemoryBlock(DbgHelp.ImageRvaToVa(pImg_NT_Header, pImg_DOS_Header,
                        pImg_Export_Dir.asLONG(), NullPointer.NULL).getValue(), IMAGE_EXPORT_DIRECTORY_SIZE));

                LONG ppdwNames = new LONG(pImg_Export_Dir.getAsInt(8 * 4/* AddressOfNames */));
                ppdwNames = DbgHelp.ImageRvaToVa(pImg_NT_Header,
                        pImg_DOS_Header, ppdwNames, NullPointer.NULL);
                if (ppdwNames.getValue() == 0)
                {
                    Kernel32.UnmapViewOfFile(lpFileBase);
                    Kernel32.CloseHandle(hFileMapping);
                    Kernel32.CloseHandle(hFile);
                    getLogger().log(SEVERITY.DEBUG,
                            String.format(">>>ERROR<<< : ImageRvaToVa returned NULL\n"));
                    return null;
                }

                int iNoOfExports = pImg_Export_Dir.getAsInt(6 * 4/* NumberOfNames */);
                String[] pszFunctions = new String[iNoOfExports];

                getLogger().log(SEVERITY.DEBUG, String.format("pszFunctions = %d\n",
                        pszFunctions.length));

                for (int i = 0, ippdwNames = ppdwNames.getValue(); i < iNoOfExports; i++, ippdwNames += 4)
                {
                    getLogger().log(SEVERITY.DEBUG, String.format("ippdwNames[%d] : %d\n", i,
                            ippdwNames));
                    LONG szFunc = DbgHelp.ImageRvaToVa(pImg_NT_Header,
                            pImg_DOS_Header, new LONG(new Pointer(
                            new NativeMemoryBlock(ippdwNames, 4)).getAsInt(0)), NullPointer.NULL);
                    pszFunctions[i] = new Pointer(new NativeMemoryBlock(szFunc.getValue(), 1000)).getAsString().trim();

                    getLogger().log(SEVERITY.DEBUG, pszFunctions[i]);
                }
                Kernel32.UnmapViewOfFile(lpFileBase);
                Kernel32.CloseHandle(hFileMapping);
                Kernel32.CloseHandle(hFile);
                return pszFunctions;

            }
            catch (IllegalAccessException e)
            {
                getLogger().log(SEVERITY.ERROR, e);
                return null;
            }

        }
        else if (isLinux)
        {
            try
            {
                String option = "";
                if (demangled)
                {
                    option = "C";
                }
                Process p = Runtime.getRuntime().exec(
                        "/usr/bin/nm -" + option + "Dg --defined-only " + dllFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = br.readLine();
                ArrayList<String> l = new ArrayList<String>();
                while (line != null)
                {
                    l.add(line);
                    line = br.readLine();
                }
                br.close();
                getLogger().log(SEVERITY.DEBUG, "exit value : " + p.waitFor());
                String[] array = new String[l.size()];
                int i = 0;
                for (String s : l)
                {
                    array[i++] = s;
                }
                return array;
            }
            catch (IOException e)
            {
                getLogger().log(SEVERITY.ERROR, e);
                throw new NativeException(e.getMessage());
            }
        }
        else
        {
            return null;
        }
    }

    // copies source-pointer memory to dest-pointer
    // if dest is not large enough only the portion of source is copied that fits into dest
    public static void copyMemory(Pointer source, Pointer dest) throws NativeException
    {
        if (dest == null || source == null || source.getSize() == 0 || dest.getSize() == 0)
        {
            return;
        }
        if (dest.getSize() >= source.getSize())
        {
            dest.setMemory(source.getMemory());
        }
        else
        {
            dest.setMemory(MyJNative.getMemory(source.getPointer(), dest.getSize()));
        }
    }

    /**
     * Method getDLLFileExports gets all the names of the functions exported by
     * a library
     *
     * @param dllFile
     *            the name of a library
     *
     * @return a String[] the names of the functions
     *
     * @exception NativeException
     *
     */
    public static String[] getDLLFileExports(String dllFile)
            throws NativeException, InterruptedException
    {
        return getDLLFileExports(dllFile, false);
    }

    public static int searchNativePattern(int nativePointer, byte[] pattern,
            int maxSize) throws NativeException
    {
        return nGetNativePattern(nativePointer, pattern, maxSize);
    }

    public static int searchNativePattern(Pointer pointer, byte[] pattern,
            int maxSize) throws NativeException
    {
        return nGetNativePattern(pointer.getPointer(), pattern, maxSize);
    }

    public static String getNativeSideVersion() throws NativeException
    {
        return nGetNativeSideVersion();
    }
    // Thubby: I am not really sure what this is for
    private static List<String> sides = null;

    public static List<String> getCompatibleNativeVersion()
    {
        if (sides == null)
        {
            sides = new ArrayList<String>();
            sides.add("1.3.2");
            sides.add("1.4");
        }
        return sides;
    }

    /**
     * Returns true if the current running platform is Windows
     *
     * @return a boolean
     *
     * @version 1/22/2006
     */
    public static boolean isWindows()
    {
        return isWindows;
    }

    /**
     * Returns true if the current running platform is Linux
     *
     * @return a boolean
     *
     * @version 1/22/2006
     */
    public static boolean isLinux()
    {
        return isLinux;
    }

    public static void setLogger(JNativeLogger _logger)
    {
        mLogger = _logger;
    }

    public static JNativeLogger getLogger()
    {
        if (mLogger == null)
        {
            mLogger = ConsoleLogger.getInstance(MyJNative.class);
        }
        return mLogger;
    }

    public static void setLoggingEnabled(boolean b)
    {
        DEBUG = b;
    }

    public static boolean isLogginEnabled()
    {
        return DEBUG;
    }

    /**
     * Must be called if running with the property <i>jnative.loadNative=<b>manual</b></i>
     * @deprecated this method does nothing now
     * @throws NativeException
     */
    @Deprecated
    public static void initCallbacks() throws NativeException
    {
    }

    /**
     * <p>
     * This method does nothing!
     * </p>
     * @deprecated this method does nothing

     * @exception NativeException
     * @exception IllegalAccessException
     *                if <code>dispose()</code> have already been called.
     *
     */
    @Deprecated
    public final void dispose() throws NativeException, IllegalAccessException
    {
        /*
        throwClosed();

        synchronized (mLibs)
        {
        LibDesc libDesc = getLibDesc(mDllName);
        libDesc.numHolders--;
        if(libDesc.numHolders == 0)
        {
        nDispose(mJNativeHModule);
        mIsClosed = true;
        mLibs.remove(mDllName);
        }
        }
         */
    }

    /**
     * Method getAvailableCallbacks
     *
     *
     * @return the number of callback you can create before JNative runs out
     *         mCallbacks.
     * @deprecated this method returns always 1000
     */
    public static int getAvailableCallbacks()
    {
        return 1000;
    }
}
