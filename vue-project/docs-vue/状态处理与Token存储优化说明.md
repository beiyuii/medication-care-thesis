# 状态处理与 Token 存储优化说明

## 修复的问题

### ✅ 1. localStorage 初始化缺少异常处理

**问题**：
- `JSON.parse(localStorage.getItem('auth_user') || 'null')` 如果 localStorage 中有无效 JSON，会抛出异常
- 直接访问 `localStorage` 在某些环境下可能不存在

**修复**：
- 创建 `src/utils/storage.ts` 统一处理 localStorage 操作
- 添加环境检查和异常处理
- 在解析用户信息时添加 try-catch，解析失败时清除无效数据

**代码位置**：`src/stores/auth.ts` 第 37-55 行

### ✅ 2. switchRole 方法未持久化

**问题**：
- `switchRole()` 只更新内存中的 role，没有更新 localStorage
- 刷新页面后角色会恢复

**修复**：
- 在 `switchRole()` 中添加 localStorage 持久化
- 添加异常处理

**代码位置**：`src/stores/auth.ts` 第 101-109 行

### ✅ 3. localStorage 访问缺少环境检查

**问题**：
- 在 SSR 或某些环境下，`localStorage` 可能不存在
- 直接访问会报错

**修复**：
- 创建 `src/utils/storage.ts` 工具函数
- 所有 localStorage 操作都通过该工具函数
- 添加 `typeof window !== 'undefined'` 检查

**代码位置**：`src/utils/storage.ts`

### ✅ 4. Token 验证缺失

**问题**：
- 应用启动时没有验证 token 是否有效
- 如果 token 过期，用户仍会看到已登录状态，直到接口返回 401

**修复**：
- 在 `src/main.ts` 中添加 `validateToken()` 函数
- 应用启动时异步验证 token
- Token 无效时自动清除会话

**代码位置**：`src/main.ts` 第 16-46 行

### ✅ 5. setSession 和 clearSession 缺少异常处理

**问题**：
- localStorage 操作可能失败（存储空间已满、隐私模式等）
- 没有异常处理，可能导致应用崩溃

**修复**：
- 所有 localStorage 操作都添加 try-catch
- 即使持久化失败，内存中的状态仍然有效
- 输出警告日志便于调试

**代码位置**：
- `src/stores/auth.ts` 第 66-80 行（setSession）
- `src/stores/auth.ts` 第 84-97 行（clearSession）

## 新增文件

### `src/utils/storage.ts`

提供安全的 localStorage 操作接口：

```typescript
export const storage = {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
  clear(): void
}
```

**特性**：
- ✅ 环境检查（SSR 兼容）
- ✅ 异常处理
- ✅ 统一接口

## 修改的文件

### 1. `src/stores/auth.ts`

**改动**：
- 导入 `storage` 工具函数
- 初始化 state 时使用安全的 `parseUser()` 函数
- `setSession()` 添加异常处理
- `clearSession()` 添加异常处理
- `switchRole()` 添加持久化

### 2. `src/main.ts`

**改动**：
- 添加 `validateToken()` 函数
- 应用启动时验证 token 有效性
- Token 无效时自动清除会话

## 优化效果

### 1. 健壮性提升

- ✅ 所有 localStorage 操作都有异常处理
- ✅ 环境兼容性更好（SSR、隐私模式等）
- ✅ 无效数据自动清理

### 2. 用户体验提升

- ✅ Token 过期自动处理，无需等待接口返回 401
- ✅ 角色切换持久化，刷新页面不丢失
- ✅ 错误不会导致应用崩溃

### 3. 可维护性提升

- ✅ localStorage 操作统一管理
- ✅ 代码更清晰，职责分离
- ✅ 便于后续扩展（如添加 sessionStorage 支持）

## 测试建议

### 测试 1：localStorage 异常处理

1. 打开浏览器开发者工具
2. 进入 Application → Local Storage
3. 手动修改 `auth_user` 为无效 JSON（如 `"invalid"`）
4. 刷新页面
5. **预期结果**：
   - 应用正常启动
   - 控制台输出警告信息
   - `auth_user` 被清除
   - 用户状态为未登录

### 测试 2：Token 验证

1. 登录系统
2. 在浏览器控制台手动修改 `auth_token` 为无效值
3. 刷新页面
4. **预期结果**：
   - 应用启动时验证 token
   - Token 无效，自动清除会话
   - 跳转到登录页

### 测试 3：角色切换持久化

1. 登录系统（假设是护工角色）
2. 调用 `authStore.switchRole('child')`
3. 刷新页面
4. **预期结果**：
   - 角色保持为 `child`
   - 导航菜单显示子女角色的菜单

### 测试 4：存储空间已满

1. 使用浏览器扩展或脚本填满 localStorage
2. 尝试登录
3. **预期结果**：
   - 登录成功（内存状态有效）
   - 控制台输出警告信息
   - 刷新页面后状态丢失（因为无法持久化）

## 注意事项

1. **Token 验证是异步的**：应用启动时不会阻塞，验证在后台进行
2. **内存状态优先**：即使 localStorage 操作失败，内存中的状态仍然有效
3. **错误日志**：所有 localStorage 错误都会输出到控制台，便于调试

## 后续优化建议

1. **Token 刷新机制**：在 token 即将过期时自动刷新
2. **SessionStorage 支持**：对于敏感信息，考虑使用 sessionStorage
3. **存储加密**：对敏感信息进行加密存储
4. **存储配额监控**：监控 localStorage 使用情况，提前预警

## 总结

✅ **所有状态处理和 Token 存储问题已修复**

- localStorage 操作统一管理，异常处理完善
- Token 验证机制完善，自动处理过期情况
- 角色切换持久化，用户体验更好
- 代码健壮性提升，不会因存储问题崩溃

**现在应用的状态管理和 Token 存储更加健壮可靠！** 🎉

