/*******************************************************************************
 * 
 * Copyright (c) 2008 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id$
 *     
 *******************************************************************************/
package de.innot.avreclipse.core.toolinfo.fuses;

/**
 * Abstract container for byte values.
 * <p>
 * This class is the base for the {@link FuseByteValues} and {@link LockbitsByteValues} classes.
 * </p>
 * <p>
 * It manages an arbitrary array of byte values and knows for which MCU these byte values are valid.
 * It is up to the subclasses to assign a meaning to these byte value
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public abstract class ByteValues {

	/** The MCU for which the byte values are valid. Set during instantiation. */
	private final String	fMCUId;

	/** The number of bytes in this object */
	private final int		fByteCount;

	/** The actual byte values. The array is initialized during instantiation. */
	private final int[]		fValues;

	/**
	 * Create a new byte values container for a given MCU.
	 * <p>
	 * The MCU parameter is stored but only used for reference. The actual number of bytes does not
	 * depend on the MCU but is taken from the subclass via the {@link #getByteCount()} hook method.
	 * </p>
	 * 
	 * @param mcuid
	 *            <code>String</code> with a MCU id value.
	 */
	protected ByteValues(String mcuid) {
		fMCUId = mcuid;
		fByteCount = getByteCount();
		fValues = new int[fByteCount];
		clearValues();
	}

	/**
	 * Clone constructor.
	 * <p>
	 * Creates a new byte values container and copies all values (and the MCU id) from the source.
	 * </p>
	 * 
	 * @param source
	 *            <code>ByteValues</code> object to clone.
	 */
	protected ByteValues(ByteValues source) {
		fMCUId = source.fMCUId;
		fByteCount = source.fByteCount;
		fValues = new int[fByteCount];
		System.arraycopy(source.fValues, 0, fValues, 0, fByteCount);
	}

	/**
	 * Create a new byte values container for the given MCU id and copies the values of the given
	 * source ByteValues object to the new ByteValues object.
	 * <p>
	 * If the source has more values, they are truncated. If it has less then the values array is
	 * filled up with <code>-1</code> values.
	 * </p>
	 * 
	 * @param mcuid
	 * @param source
	 */
	protected ByteValues(String mcuid, ByteValues source) {
		fMCUId = mcuid;
		fByteCount = getByteCount();
		fValues = new int[fByteCount];
		for (int i = 0; i < fValues.length; i++) {
			fValues[i] = -1;
		}
		int copycount = Math.min(fByteCount, source.fByteCount);
		System.arraycopy(source.fValues, 0, fValues, 0, copycount);
	}

	/**
	 * Get the current MCU for which the byte values are valid.
	 * 
	 * @return <code>String</code> with a MCU id.
	 */
	public String getMCUId() {
		return fMCUId;
	}

	/**
	 * Sets the byte at the given index to a value.
	 * 
	 * @param index
	 *            The index of the byte to set. Must be between 0 and <code>getMaxBytes() - 1</code>.
	 * @param value
	 *            The new value. Must be a byte value (0-255) or -1 to unset the value.
	 * @throws IllegalArgumentException
	 *             if the index is out of bounds or the value is out of range.
	 */
	public void setValue(int index, int value) {

		checkIndex(index);

		fValues[index] = value;
	}

	/**
	 * Get the value of the byte with the given index.
	 * 
	 * @param index
	 *            The index of the byte to read. Must be between 0 and
	 *            <code>getMaxBytes() - 1</code>.
	 * @return <code>int</code> with a byte value (0-255) or <code>-1</code> if the value is not
	 *         set.
	 * @throws IllegalArgumentException
	 *             if the index is out of bounds.
	 */
	public int getValue(int index) {
		checkIndex(index);

		return fValues[index];
	}

	/**
	 * Set all byte values.
	 * <p>
	 * Copies all values from the given array to the internal storage. If the given
	 * <code>int[]</code> has less entries than this class supports, then the remaining bytes
	 * remain untouched. Only <code>getMaxBytes()</code> bytes are copied. Any additional bytes in
	 * the source array are ignored.
	 * </p>
	 * <p>
	 * The values of the source are not checked.
	 * </p>
	 * 
	 * @param newvalues
	 *            Array of <code>int</code> with the new byte values (0 to 255 or -1).
	 */
	public void setValues(int[] newvalues) {
		int count = Math.min(newvalues.length, fValues.length);
		System.arraycopy(newvalues, 0, fValues, 0, count);
	}

	/**
	 * Returns an array with all byte values.
	 * <p>
	 * The returned array is a copy of the internal structure.
	 * </p>
	 * 
	 * @return Array of <code>int</code> with the current byte values (0 to 255 or -1).
	 */
	public int[] getValues() {
		// make a copy and return it
		int[] copy = new int[fValues.length];
		System.arraycopy(fValues, 0, copy, 0, fValues.length);
		return copy;
	}

	/**
	 * Clears all values.
	 * <p>
	 * This method will set the value of all bytes to <code>-1</code>
	 * </p>
	 */
	public void clearValues() {
		for (int i = 0; i < fValues.length; i++) {
			fValues[i] = -1;
		}
	}

	/**
	 * Get the actual number of bytes supported by the MCU.
	 * <p>
	 * While the <code>getValues()</code> method always returns the maximum number of bytes,
	 * independent of the MCU, this method gets the number of bytes supported by the current MCU.
	 * </p>
	 * <p>
	 * If the MCU is not supported <code>0</code> is returned.
	 * </p>
	 * 
	 * @return Number of bytes supported by the MCU. Between <code>0</code> and
	 *         <code>getMaxBytes()</code>.
	 */
	public abstract int getByteCount();

	/**
	 * Checks if the index is valid for the subclass.
	 * 
	 * @param index
	 *            Index value to test.
	 * @throws IllegalArgumentException
	 *             if the index is not valid.
	 */
	private void checkIndex(int index) {
		if (!(0 <= index && index < getByteCount())) {
			throw new IllegalArgumentException("[" + index + "] is not a valid byte index.");
		}
	}
}
