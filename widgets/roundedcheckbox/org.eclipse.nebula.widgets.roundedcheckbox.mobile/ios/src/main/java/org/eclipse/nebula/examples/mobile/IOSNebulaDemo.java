// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

/**
 * iOS application main for the merged SWT IOSPlatform.launch bootstrap.
 * The native iOS application supplies a supported Java runtime and invokes
 * IOSPlatform.launch(IOSNebulaDemo.class.getName(), args) on its SWT worker.
 */
public final class IOSNebulaDemo {
    private IOSNebulaDemo() { }
    public static void main(String[] args) {
        NebulaMobileMain.main(args);
    }
}
