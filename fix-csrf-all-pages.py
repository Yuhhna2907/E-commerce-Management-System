#!/usr/bin/env python3
"""
Auto-fix CSRF for all user pages
Adds CSRF meta tags and csrf-auto.js script
"""

import os
import re

# Pages to fix
PAGES = [
    'src/main/resources/templates/user/wishlist/list.html',
    'src/main/resources/templates/user/saved/list.html',
    'src/main/resources/templates/user/product/list.html',
    'src/main/resources/templates/user/product/detail.html',
    'src/main/resources/templates/user/loyalty/index.html',
    'src/main/resources/templates/user/home.html',
    'src/main/resources/templates/user/order/checkout.html',
    'src/main/resources/templates/user/profile/index.html',
]

CSRF_META = '''    <!-- CSRF Meta Tags -->
    <th:block th:replace="fragments/base-scripts :: csrf-meta"></th:block>
'''

CSRF_SCRIPT = '''<!-- CSRF Auto-Injection -->
<th:block th:replace="fragments/base-scripts :: csrf-script"></th:block>

'''

def fix_page(filepath):
    """Fix a single page"""
    if not os.path.exists(filepath):
        print(f"❌ File not found: {filepath}")
        return False
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Check if already has CSRF meta
    if 'csrf-meta' in content or '_csrf' in content:
        print(f"✅ Already has CSRF meta: {filepath}")
    else:
        # Add CSRF meta after <meta name="viewport"...>
        viewport_pattern = r'(<meta name="viewport"[^>]*>)'
        if re.search(viewport_pattern, content):
            content = re.sub(
                viewport_pattern,
                r'\1\n' + CSRF_META,
                content,
                count=1
            )
            print(f"✅ Added CSRF meta: {filepath}")
        else:
            print(f"⚠️  No viewport meta found: {filepath}")
    
    # Check if already has CSRF script
    if 'csrf-script' in content or 'csrf-auto.js' in content:
        print(f"✅ Already has CSRF script: {filepath}")
    else:
        # Add CSRF script before </body> or before last </script>
        if '</body>' in content:
            content = content.replace('</body>', CSRF_SCRIPT + '</body>', 1)
            print(f"✅ Added CSRF script before </body>: {filepath}")
        elif '</script>' in content:
            # Find last </script>
            last_script_pos = content.rfind('</script>')
            if last_script_pos != -1:
                content = content[:last_script_pos + 9] + '\n\n' + CSRF_SCRIPT + content[last_script_pos + 9:]
                print(f"✅ Added CSRF script after last </script>: {filepath}")
        else:
            print(f"⚠️  No </body> or </script> found: {filepath}")
    
    # Only write if changed
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"💾 Saved: {filepath}\n")
        return True
    else:
        print(f"⏭️  No changes needed: {filepath}\n")
        return False

def main():
    print("=" * 60)
    print("CSRF Auto-Fix Script")
    print("=" * 60)
    print()
    
    fixed_count = 0
    for page in PAGES:
        if fix_page(page):
            fixed_count += 1
    
    print("=" * 60)
    print(f"✅ Fixed {fixed_count}/{len(PAGES)} pages")
    print("=" * 60)

if __name__ == '__main__':
    main()
