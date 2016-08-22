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

	public int BaseAddress; // �������ַ��
	public int AllocationBase; // �������ַ��
	public int AllocationProtect; // ���򱻳��α���ʱ����ı������ԡ�
	public int RegionSize; // �����С�����ֽ�Ϊ������λ����
	public int State; // ״̬��MEM_FREE��MEM_RESERVE�� MEM_COMMIT����
	public int Protect; // �������ԡ�
	public int Type; // ���͡�

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