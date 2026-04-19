#!/usr/bin/env python3
"""快速测试应用是否能正常启动。"""

import sys

def main() -> int:
    try:
        from app.main import create_app

        app = create_app()
        print("✅ 应用创建成功！")
        print(f"✅ 模板文件夹: {app.template_folder}")
        print(f"✅ 路由数量: {len(app.url_map._rules)}")
        print("\n可用的路由:")
        for rule in app.url_map.iter_rules():
            print(f"  - {rule.rule} [{', '.join(rule.methods)}]")
        print("\n✅ 所有检查通过！可以启动服务了。")
        return 0
    except Exception as exc:
        print(f"❌ 错误: {exc}")
        import traceback

        traceback.print_exc()
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
