# KnowledgeFlow - AI驱动的知识聚合与学习管理平台

<div align="center">

![KnowledgeFlow Logo](https://img.shields.io/badge/KnowledgeFlow-v0.1.0-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Vue](https://img.shields.io/badge/Vue-3.x-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

**一站式知识管理平台 | 智能学习助手 | 个人成长跟踪**

[功能特性](#功能特性) · [技术架构](#技术架构) · [快速开始](#快速开始) · [开发指南](#开发指南) · [贡献指南](#贡献指南)

</div>

---

## 📖 项目简介

KnowledgeFlow 是一个面向开发者的智能知识管理平台，集成了多源内容聚合、AI辅助学习、知识图谱可视化、间隔重复记忆等功能，帮助开发者高效管理和学习技术知识。

### 核心价值

- **内容聚合**：统一管理GitHub、arXiv、技术博客、个人笔记等多源内容
- **智能学习**：AI辅助学习路径规划、智能问答、间隔重复学习
- **知识管理**：双向链接、知识图谱、全文检索、语义检索
- **成长追踪**：学习数据可视化、技能树展示、年度报告生成
- **高效输出**：博客生成、面试准备、项目孵化

---

## ✨ 功能特性

### 🔄 内容聚合
- GitHub仓库订阅与同步
- arXiv论文检索与导入
- RSS订阅源管理
- 文件上传支持（PDF、Markdown、HTML）
- 智能内容分类与标签提取

### 🤖 AI智能助手
- **RAG智能问答**：基于个人知识库的精准问答
- **学习路径规划**：AI生成个性化学习计划
- **概念解释**：技术术语多层次解释
- **主动推荐**：知识关联推荐与遗忘提醒

### 📝 笔记管理
- Markdown实时预览与编辑
- 双向链接（`[[笔记标题]]`语法）
- 版本历史追踪
- 图片拖拽上传
- 代码高亮与数学公式支持

### 🕸️ 知识图谱
- 技术栈知识图谱可视化
- 个人知识图谱自动构建
- 知识密度分析
- 学习路径推荐
- 知识盲区识别

### 📚 学习管理
- 学习任务规划与追踪
- 番茄钟专注计时
- 学习记录统计
- 进度可视化

### 🧠 间隔重复学习
- SM-2算法实现
- 智能复习提醒
- 知识卡片管理
- 复习效果反馈

### 📊 数据分析
- 学习时长统计
- 技能雷达图
- 学习热力图
- 知识增长曲线
- 年度学习报告

### 🌐 分享与协作
- 个人主页定制
- 邀请码系统
- 公开分享控制
- 访客统计

---

## 🛠️ 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 核心开发语言 |
| Spring Boot | 3.5.9 | 应用框架 |
| Spring Security | 6.1 | 认证授权 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0 | 主数据库 |
| Redis | 7.0 | 缓存与会话 |
| Elasticsearch | 8.x | 全文检索 |
| Neo4j | 5.x | 知识图谱 |
| Qdrant | latest | 向量数据库 |
| MinIO | latest | 对象存储 |
| RabbitMQ | 3.x | 消息队列 |
| LangChain4j | 0.27.1 | AI集成框架 |
| HanLP | 1.8.4 | 中文NLP |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| Vite | 5.x | 构建工具 |
| Element Plus | latest | UI组件库 |
| Vditor | latest | Markdown编辑器 |
| ECharts | 5.x | 数据可视化 |
| D3.js | latest | 图谱可视化 |

---

## 🚀 快速开始

### 环境要求

- **Java**: 17+
- **Node.js**: 18+
- **Docker**: 20+
- **Maven**: 3.8+
- **系统资源**: 16GB RAM（推荐）

### 1. 克隆项目

```bash
git clone https://github.com/Ray-Hx/knowlege-flow.git
cd knowlege-flow
```

### 2. 启动基础设施

使用Docker Compose启动所有依赖服务：

```bash
# 启动所有服务
./start.sh

# 或手动启动
docker-compose up -d
```

服务启动后可访问：
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Elasticsearch: `http://localhost:9200`
- Neo4j Browser: `http://localhost:7474`
- MinIO Console: `http://localhost:9001`
- RabbitMQ Management: `http://localhost:15672`

### 3. 配置环境变量

复制并编辑环境配置：

```bash
cp .env.example .env
# 编辑 .env 文件，配置数据库密码、API密钥等
```

**重要配置项**：
- `OPENAI_API_KEY`: OpenAI API密钥（用于AI功能）
- `GITHUB_CLIENT_ID`: GitHub OAuth客户端ID（用于第三方登录）
- `JWT_SECRET`: JWT签名密钥（生产环境必须修改）

### 4. 启动后端

```bash
# 安装依赖并启动
./mvnw spring-boot:run

# 或使用IDE启动 KnowledgeFlowApplication.java
```

后端启动后访问：
- API文档: `http://localhost:8080/api/swagger-ui.html`
- 健康检查: `http://localhost:8080/api/actuator/health`

### 5. 启动前端（开发中）

```bash
cd frontend
npm install
npm run dev
```

---

## 📁 项目结构

```
knowledge-flow/
├── docker/                      # Docker配置
│   ├── mysql/                  # MySQL初始化脚本
│   ├── redis/                  # Redis配置
│   ├── elasticsearch/          # ES插件
│   └── neo4j/                  # Neo4j插件
├── src/main/java/com/ray/knowledgeflow/
│   ├── config/                 # 配置类
│   ├── common/                 # 公共组件
│   │   ├── constants/          # 常量定义
│   │   ├── enums/             # 枚举类型
│   │   ├── exception/         # 异常类
│   │   ├── result/            # 响应封装
│   │   └── utils/             # 工具类
│   ├── security/              # 安全组件
│   │   ├── jwt/              # JWT认证
│   │   └── oauth/            # OAuth2
│   └── module/               # 业务模块
│       ├── user/             # 用户模块
│       ├── content/          # 内容聚合
│       ├── note/             # 笔记管理
│       ├── knowledge/        # 知识图谱
│       ├── learning/         # 学习管理
│       ├── spaced/           # 间隔重复
│       ├── ai/               # AI助手
│       ├── output/           # 输出分享
│       └── system/           # 系统管理
├── src/main/resources/
│   ├── application.yml       # 主配置文件
│   └── mapper/              # MyBatis映射
├── frontend/                # Vue前端项目
├── docker-compose.yml       # Docker编排
└── start.sh                # 启动脚本
```

---

## 🔧 开发指南

### 架构设计

项目采用分层架构：
- **Controller层**：处理HTTP请求，参数验证
- **Service层**：业务逻辑实现
- **Mapper层**：数据持久化
- **DTO层**：数据传输对象

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 统一异常处理
- RESTful API设计

### 开发流程

1. 从`develop`分支创建功能分支
2. 编写代码并提交（遵循Conventional Commits）
3. 编写单元测试
4. 提交Pull Request
5. Code Review后合并

### 环境配置

- **开发环境**：`application.yml` (profile: dev)
- **测试环境**：`application-test.yml`
- **生产环境**：`application-prod.yml`

---

## 📚 API文档

启动项目后访问 Swagger UI：
```
http://localhost:8080/api/swagger-ui.html
```

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

### Commit规范

使用 Conventional Commits：
- `feat`: 新功能
- `fix`: 错误修复
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具链

---

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

## 👨‍💻 作者

**Ray Huang**
- GitHub: [@Ray-Hx](https://github.com/Ray-Hx)

---

## 🙏 致谢

感谢以下开源项目：
- Spring Boot
- Vue.js
- LangChain4j
- Element Plus
- 其他所有依赖项目

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐️ Star！**

Made with ❤️ by Ray Huang

</div>
