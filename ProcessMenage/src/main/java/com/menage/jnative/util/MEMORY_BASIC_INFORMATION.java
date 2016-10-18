package com.menage.jnative.util;

import org.xvolks.jnative.exceptions.NativeException;
import org.xvolks.jnative.misc.basicStructures.AbstractBasicData;
import org.xvolks.jnative.pointers.Pointer;
import org.xvolks.jnative.pointers.memory.MemoryBlockFactory;


public class MEMORY_BASIC_INFORMATION extends AbstractBasicData<MEMORY_BASIC_INFORMATION> {
	public MEMORY_BASIC_INFORMATION() {
		super(null);
		try {
			createPointer();
		} catch (NativeException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public int BaseAddress; // 区域基地址。
	public int AllocationBase; // 分配基地址。
	public int AllocationProtect; // 区域被初次保留时赋予的保护属性。
	public int RegionSize; // 区域大小（以字节为计量单位）。
	public int State; // 状态（MEM_FREE、MEM_RESERVE或 MEM_COMMIT）。
	public int Protect; // 保护属性。
	public int Type; // 类型。

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