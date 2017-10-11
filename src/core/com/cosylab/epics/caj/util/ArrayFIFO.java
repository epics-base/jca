/*
 * Copyright (c) 2009 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package com.cosylab.epics.caj.util;

/**
 * FIFO "stack".
 */
public class ArrayFIFO
{
    private Object[] elements;

    private int head;

    private int tail;

    private static final int MIN_INITIAL_CAPACITY = 8;

    /**
     * Allocate empty array to hold the given number of elements.
     * @param numElements  the number of elements to hold
     */
    private void allocateElements(int numElements) {
        int initialCapacity = MIN_INITIAL_CAPACITY;
        // Find the best power of two to hold elements.
        // Tests "<=" because arrays aren't kept full.
        if (numElements >= initialCapacity) {
            initialCapacity = numElements;
            initialCapacity |= (initialCapacity >>>  1);
            initialCapacity |= (initialCapacity >>>  2);
            initialCapacity |= (initialCapacity >>>  4);
            initialCapacity |= (initialCapacity >>>  8);
            initialCapacity |= (initialCapacity >>> 16);
            initialCapacity++;

            if (initialCapacity < 0)   // Too many elements, must back off
                initialCapacity >>>= 1;// Good luck allocating 2 ^ 30 elements
        }
        elements = new Object[initialCapacity];
    }

    /**
     * Double the capacity of this deque.  Call only when full, i.e.,
     * when head and tail have wrapped around to become equal.
     */
    private void doubleCapacity() {
        int p = head;
        int n = elements.length;
        int r = n - p; // number of elements to the right of p
        int newCapacity = n << 1;
        if (newCapacity < 0)
            throw new IllegalStateException("Sorry, deque too big");
        Object[] a = new Object[newCapacity];
        System.arraycopy(elements, p, a, 0, r);
        System.arraycopy(elements, 0, a, r, p);
        elements = a;
        head = 0;
        tail = n;
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold 16 elements.
     */
    public ArrayFIFO() {
        elements = new Object[16];
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold the specified number of elements.
     * @param numElements  lower bound on initial capacity of the deque
     */
    public ArrayFIFO(int numElements) {
        allocateElements(numElements);
    }

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    public synchronized void addFirst(Object e) {
        if (e == null)
            throw new NullPointerException();
        elements[head = (head - 1) & (elements.length - 1)] = e;
        if (head == tail)
            doubleCapacity();
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    public synchronized void addLast(Object e) {
        if (e == null)
            throw new NullPointerException();
        elements[tail] = e;
        if ( (tail = (tail + 1) & (elements.length - 1)) == head)
            doubleCapacity();
    }

    public synchronized Object pollFirst() {
        int h = head;
        Object result = elements[h]; // Element is null if deque empty
        if (result == null)
            return null;
        elements[h] = null;     // Must null out slot
        head = (h + 1) & (elements.length - 1);
        return result;
    }

    public synchronized Object pollLast() {
        int t = (tail - 1) & (elements.length - 1);
        Object result = elements[t];
        if (result == null)
            return null;
        elements[t] = null;
        tail = t;
        return result;
    }

    public synchronized Object peekFirst() {
        return elements[head]; // elements[head] is null if deque empty
    }

    public synchronized Object peekLast() {
        return elements[(tail - 1) & (elements.length - 1)];
    }

    /**
     * Pushes an element onto the stack represented by this deque.  In other
     * words, inserts the element at the front of this deque.
     *
     * @param e the element to push
     * @throws NullPointerException if the specified element is null
     */
    public synchronized void push(Object e) {
        addLast(e);
    }

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *
     * @return the element at the front of this deque (which is the top
     *         of the stack represented by this deque), <code>null</code> if no element available.
     */
    public synchronized Object pop() {
        return pollFirst();
    }

    /**
     * Looks at the object at the top of this stack without removing it 
     * from the stack. 
     * @return     the object at the top of this stack (the last item 
     *             of the <tt>Vector</tt> object). 
     */
    public synchronized Object peek() {
    	return peekFirst();
    }

    /**
     * Returns the number of elements in this deque.
     * @return the number of elements in this deque
     */
    public synchronized int size() {
        return (tail - head) & (elements.length - 1);
    }

    /**
     * Returns <tt>true</tt> if this deque contains no elements.
     *
     * @return <tt>true</tt> if this deque contains no elements
     */
    public synchronized boolean isEmpty() {
        return head == tail;
    }

    /**
     * Removes all of the elements from this deque.
     * The deque will be empty after this call returns.
     */
    public synchronized void clear() {
        int h = head;
        int t = tail;
        if (h != t) { // clear all cells
            head = tail = 0;
            int i = h;
            int mask = elements.length - 1;
            do {
                elements[i] = null;
                i = (i + 1) & mask;
            } while (i != t);
        }
    }

    /**
     * Removes the first occurrence of the specified element in this
     * deque (when traversing the deque from head to tail).
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element <tt>e</tt> such that
     * <tt>o.equals(e)</tt> (if such an element exists).
     * Returns <tt>true</tt> if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return <tt>true</tt> if the deque contained the specified element
     */
    public synchronized boolean remove(Object o) {
        if (o == null)
            return false;
        int mask = elements.length - 1;
        int i = head;
        Object x;
        while ( (x = elements[i]) != null) {
            if (o.equals(x)) {
                delete(i);
                return true;
            }
            i = (i + 1) & mask;
        }
        return false;
    }

    /**
     * Removes the element at the specified position in the elements array,
     * adjusting head and tail as necessary.  This can result in motion of
     * elements backwards or forwards in the array.
     *
     * <p>This method is called delete rather than remove to emphasize
     * that its semantics differ from those of {@link List#remove(int)}.
     *
     * @return true if elements moved backwards
     */
    private boolean delete(int i) {
        final Object[] elements = this.elements;
        final int mask = elements.length - 1;
        final int h = head;
        final int t = tail;
        final int front = (i - h) & mask;
        final int back  = (t - i) & mask;

        // Invariant: head <= i < tail mod circularity
        if (front >= ((t - h) & mask))
            throw new IllegalStateException();	// concurrency problem!!! 

        // Optimize for least element motion
        if (front < back) {
            if (h <= i) {
                System.arraycopy(elements, h, elements, h + 1, front);
            } else { // Wrap around
                System.arraycopy(elements, 0, elements, 1, i);
                elements[0] = elements[mask];
                System.arraycopy(elements, h, elements, h + 1, mask - h);
            }
            elements[h] = null;
            head = (h + 1) & mask;
            return false;
        } else {
            if (i < t) { // Copy the null tail as well
                System.arraycopy(elements, i + 1, elements, i, back);
                tail = t - 1;
            } else { // Wrap around
                System.arraycopy(elements, i + 1, elements, i, mask - i);
                elements[mask] = elements[0];
                System.arraycopy(elements, 1, elements, 0, t);
                tail = (t - 1) & mask;
            }
            return true;
        }
    }
}