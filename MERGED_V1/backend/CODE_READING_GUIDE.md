# 后端代码阅读顺序与分层说明

## 1. 从哪里开始

入口类：

```text
src/main/java/com/smartproperty/platform/SmartPropertyApplication.java
```

建议按一条 HTTP 请求的真实调用路径阅读：

```text
Vue 页面
  → Controller（接收和校验 HTTP 参数）
  → Service（权限后的业务规则、事务、乐观锁、审计）
  → Mapper 接口（数据访问边界）
  → resources/mapper/*.xml（SQL）
  → MySQL
```

## 2. 当前包结构

项目采用“按业务域分包”，而不是把全系统所有 Controller、Service 都堆在全局同名目录：

```text
com.smartproperty.platform
├─ auth       登录、JWT、刷新会话、启动账号
├─ asset      小区、楼栋、单元、房产
├─ file       户型图元数据与 MinIO
├─ config     Security、CORS、MinIO、JSON 配置
└─ common     统一异常和分页响应
```

这种结构更适合模块不断增加的长期项目。每个业务域内部仍然遵守 Controller → Service → Repository/Mapper 的方向，业务 Service 不反向依赖 Web 层。

## 3. 三条最适合入门的链路

### 登录链路

```text
auth/AuthController.java
→ auth/AuthService.java
→ auth/AuthMapper.java
→ resources/mapper/AuthMapper.xml
```

JWT 的签发和验证继续看：

```text
auth/JwtService.java
config/SecurityConfig.java
```

### 资产链路

```text
asset/AssetController.java
→ asset/AssetService.java
→ asset/AssetMapper.java
→ resources/mapper/AssetMapper.xml
```

这里可以看到参数校验、分页、草稿规则、乐观锁和变更审计。

### 户型图链路

```text
file/FloorPlanController.java
→ file/FloorPlanService.java
→ file/FileObjectMapper.java
→ resources/mapper/FileObjectMapper.xml
→ MinIO
```

## 4. 为什么没有手写 MapperImpl

`AssetMapper`、`AuthMapper`、`FileObjectMapper` 是 MyBatis Mapper 接口，运行时实现由 MyBatis 生成，SQL 放在 XML 中。再手写一个只负责转调 Mapper 的 `RepositoryImpl` 不会增加业务价值。

Service 目前也只有一个实现，因此直接使用具体 Service。以后出现多个实现（例如 MinIO 与其他对象存储切换、不同计费策略）时，再提取接口和实现类。

## 5. 当前需要继续演进的地方

`asset/AssetModels.java` 为 Phase 2A 小范围交付集中保存了请求和响应 record。随着 Phase 2B 加入合同、消息、中介和个人用户，应在各业务域下拆为 `dto/request`、`dto/response`，避免单文件继续膨胀。

这是一项已知的代码组织改进，不应通过堆叠空接口来掩盖；拆分时必须保持 Controller、Service、Mapper 的单向依赖和现有回归测试通过。
