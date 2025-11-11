#!/usr/bin/env python3
"""
Generate simple JUnit 5 smoke tests for Java classes under the backend module.

Rules:
- For each .java source file, read the package and class name.
- Create a test class under src/test/java with a single test that loads the class
  without initializing static initializers and, only if the class has a public
  no-arg constructor, instantiate it.
- Skip inner classes and anonymous classes.

This generator is conservative to avoid running Spring context or heavy static
initializers.
"""
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'apps' / 'backend'
SRC = ROOT / 'src' / 'main' / 'java'
TEST_ROOT = ROOT / 'src' / 'test' / 'java'

JAVA_FILE_RE = re.compile(r'public\s+(?:class|enum|interface|record)\s+(\w+)')
PACKAGE_RE = re.compile(r'^\s*package\s+([\w\.]+)\s*;', re.MULTILINE)

def find_java_files(root: Path):
    for p in root.rglob('*.java'):
        # skip generated folders if any
        if 'target' in p.parts:
            continue
        yield p

def parse_package_and_class(path: Path):
    text = path.read_text(encoding='utf-8')
    m = PACKAGE_RE.search(text)
    pkg = m.group(1) if m else None
    m2 = JAVA_FILE_RE.search(text)
    cls = m2.group(1) if m2 else None
    return pkg, cls

TEST_TEMPLATE = '''package {package};

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class {className}GeneratedTest {{

    @Test
    void smoke_loadsClass_and_optionalInstantiate() throws Exception {{
        Class<?> cls = Class.forName("{fullName}", false, Thread.currentThread().getContextClassLoader());
        assertNotNull(cls);
        try {{
            Constructor<?> ctor = cls.getDeclaredConstructor();
            // only instantiate if public no-arg to avoid heavy setups
            if (Modifier.isPublic(ctor.getModifiers()) && ctor.getParameterCount() == 0) {{
                Object inst = ctor.newInstance();
                assertNotNull(inst);
            }}
        }} catch (NoSuchMethodException ignored) {{
            // no no-arg ctor; that's fine for smoke test
        }}
    }}
}}
'''

def main():
    created = 0
    for java_file in find_java_files(SRC):
        pkg, cls = parse_package_and_class(java_file)
        if not pkg or not cls:
            continue
        # skip inner classes (filename may contain $) though java files don't
        # generate inner class files usually; keep it simple.
        full = f"{pkg}.{cls}"
        test_pkg_path = TEST_ROOT / Path(pkg.replace('.', os.sep))
        test_pkg_path.mkdir(parents=True, exist_ok=True)
        test_file = test_pkg_path / f"{cls}GeneratedTest.java"
        if test_file.exists():
            continue
        content = TEST_TEMPLATE.format(package=pkg, className=cls, fullName=full)
        test_file.write_text(content, encoding='utf-8')
        created += 1

    print(f"Generated {created} test files under {TEST_ROOT}")

if __name__ == '__main__':
    main()
