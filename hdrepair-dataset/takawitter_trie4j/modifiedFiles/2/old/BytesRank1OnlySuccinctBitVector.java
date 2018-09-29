/*
 * Copyright 2012 Takao Nakaguchi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trie4j.bv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Arrays;

public class BytesRank1OnlySuccinctBitVector
implements Externalizable, SuccinctBitVector{
	public BytesRank1OnlySuccinctBitVector(){
		this(16);
	}

	public BytesRank1OnlySuccinctBitVector(int initialCapacity){
		vector = new byte[containerCount(initialCapacity, 8)];
		countCache1 = new int[containerCount(vector.length, CACHE_WIDTH / 8)];
	}

	public BytesRank1OnlySuccinctBitVector(byte[] bytes, int bits){
		this.size = bits;
		this.vector = Arrays.copyOf(bytes, containerCount(bits, 8));
		this.countCache1 = new int[containerCount(vector.length, 8)];
		int sum = BITCOUNTS1[bytes[0] & 0xff];
		int n = vector.length;
		for(int i = 1; i < n; i++){
			if(i % 8 == 0) countCache1[(i / 8) - 1] = sum;
			sum += BITCOUNTS1[bytes[i] & 0xff];
		}
		if(countCache1.length > 0){
			countCache1[n / 8] = sum;
		}
	}

	public BytesRank1OnlySuccinctBitVector(byte[] vector, int size, int[] countCache1) {
		this.vector = vector;
		this.size = size;
		this.countCache1 = countCache1;
	}

	public byte[] getVector() {
		return vector;
	}

	public int[] getCountCache1() {
		return countCache1;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		int n = Math.min(size, 32);
		for(int i = 0; i < n; i++){
			b.append((vector[(i / 8)] & (0x80 >> (i % 8))) != 0 ? "1" : "0");
		}
		return b.toString();
	}

	@Override
	public boolean get(int pos) {
		return isOne(pos);
	}

	@Override
	public boolean isZero(int pos){
		return (vector[pos / 8] & BITS[pos % 8]) == 0;
	}

	@Override
	public boolean isOne(int pos) {
		return (vector[pos / 8] & BITS[pos % 8]) != 0;
	}

	@Override
	public int size(){
		return this.size;
	}

	public void trimToSize(){
		int vectorSize = size / 8 + 1;
		vector = Arrays.copyOf(vector, Math.min(vector.length, vectorSize));
		int blockSize = CACHE_WIDTH / 8;
		int size = vectorSize / blockSize + (((vectorSize % blockSize) != 0) ? 1 : 0);
		int countCacheSize0 = size;
		countCache1 = Arrays.copyOf(countCache1, Math.min(countCache1.length, countCacheSize0));
	}

	public void append1(){
		int i = size / 8;
		int ci = size / CACHE_WIDTH;
		prepareAppend(i, ci);
		countCache1[ci]++;
		int r = size % 8;
		vector[i] |= BITS[r];
		size++;
	}

	public void append0(){
		int i = size / 8;
		int ci = size / CACHE_WIDTH;
		prepareAppend(i, ci);
		size++;
	}
	
	@Override
	public int select0(int num) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int next0(int count) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int select1(int num) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int rank0(int pos) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int rank1(int pos){
		int ret = 0;
		int cn = pos / CACHE_WIDTH;
		if(cn > 0){
			ret = countCache1[cn - 1];
		}
		int n = pos / 8;
		for(int i = (cn * (CACHE_WIDTH / 8)); i < n; i++){
			ret += BITCOUNTS1[vector[i] & 0xff];
		}
		ret += BITCOUNTS1[vector[n] & MASKS[pos % 8]];
		return ret;
	}

	public void save(OutputStream os) throws IOException{
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(size);
		trimToSize();
		dos.writeInt(vector.length);
		dos.write(vector);
		dos.writeInt(countCache1.length);
		for(int e : countCache1){
			dos.writeInt(e);
		}
		dos.flush();
	}

	public void load(InputStream is) throws IOException{
		DataInputStream dis = new DataInputStream(is);
		size = dis.readInt();
		int vectorSize = dis.readInt();
		vector = new byte[vectorSize];
		dis.read(vector, 0, vectorSize);
		int size = dis.readInt();
		countCache1 = new int[size];
		for(int i = 0; i < size; i++){
			countCache1[i] = dis.readInt();
		}
	}

	private static int containerCount(int size, int unitSize){
		return size / unitSize + ((size % unitSize) != 0 ? 1 : 0);
	}

	private int prepareAppend(int i, int ci){
		if(i >= vector.length){
			extend();
		}
		if(size % CACHE_WIDTH == 0 && ci > 0){
			countCache1[ci] = countCache1[ci - 1];
		}
		return i;
	}

	@Override
	public void readExternal(ObjectInput in)
	throws IOException, ClassNotFoundException {
		vector = (byte[])in.readObject();
		size = in.readInt();
		countCache1 = (int[])in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(vector);
		out.writeInt(size);
		out.writeObject(countCache1);
	}

	private void extend(){
		int vectorSize = (int)(vector.length * 1.2) + 1;
		vector = Arrays.copyOf(vector, vectorSize);
		int blockSize = CACHE_WIDTH / 8;
		int size = vectorSize / blockSize + (((vectorSize % blockSize) != 0) ? 1 : 0);
		countCache1 = Arrays.copyOf(countCache1, size);
	}

	private static final int CACHE_WIDTH = 64;
	private byte[] vector;
	private int size;
	private int[] countCache1;

	private static final int[] MASKS = {
		0x80, 0xc0, 0xe0, 0xf0
		, 0xf8, 0xfc, 0xfe, 0xff
	};
	private static final byte[] BITS = {
		(byte)0x80, (byte)0x40, (byte)0x20, (byte)0x10
		, (byte)0x08, (byte)0x04, (byte)0x02, (byte)0x01
	};
	private static final byte[] BITCOUNTS1 = {
		0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
		4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8
	};

	private static final long serialVersionUID = -7658605229245494623L;
}
