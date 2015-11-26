/*******************************************************************************
 * Copyright (c) 2015 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.lib.helper.builder.tree;

import dk.alexandra.fresco.framework.ProtocolProducer;

/**
 * Generates the nodes of computation for a TreeCircuit. A node should do some
 * operation on two elements specified by two indices into some underlying list.
 * 
 * @author psn
 * 
 */
public interface TreeCircuitNodeGenerator {

	/**
	 * Generates a node of computation for a TreeCircuit.
	 * 
	 * @param i
	 *            index of the first input, this element is assumed to be
	 *            overwritten by the result of the computation.
	 * @param j
	 *            index of the second input.
	 * @return a circuit defining the computation of a node.
	 */
	public ProtocolProducer getNode(int i, int j);

	/**
	 * Gives the length of the underlying list.
	 * 
	 * @return the length of the underlying list.
	 */
	public int getLength();

}
