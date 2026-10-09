#!/usr/bin/env python3
"""
run_all_tests.py

Test runner for the Speed Lock X6871 unified integration framework.
Executes all unit, ABI, and capability engine verification tests.
"""

import os
import sys
import unittest

def main():
    test_dir = os.path.dirname(os.path.abspath(__file__))
    workspace_dir = os.path.abspath(os.path.join(test_dir, "..", "..", ".."))

    if workspace_dir not in sys.path:
        sys.path.insert(0, workspace_dir)

    print("======================================================================")
    print(" Speed Lock X6871 Integration Testbench Runner")
    print("======================================================================")

    loader = unittest.TestLoader()
    suite = loader.discover(start_dir=test_dir, top_level_dir=workspace_dir, pattern="test_*.py")

    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    print("======================================================================")
    print(f"Tests run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print(f"Skipped: {len(result.skipped)}")
    print("======================================================================")

    if result.wasSuccessful():
        print("ALL TESTS PASSED SUCCESSFULLY.")
        sys.exit(0)
    else:
        print("TESTS FAILED.")
        sys.exit(1)

if __name__ == "__main__":
    main()
