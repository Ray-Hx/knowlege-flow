import random
import datetime
import uuid
import os

# Configuration
NUM_USERS = 50
NUM_NOTES_PER_USER = 2  # Total 100 notes
NUM_CONTENT_ITEMS = 50
NUM_CONCEPTS = 50

OUTPUT_SQL = "mock_data_mysql.sql"
OUTPUT_CYPHER = "mock_data_neo4j.cypher"

# Data Sources (Chinese)
USERNAMES = [
    "张伟", "王芳", "李娜", "刘洋", "陈静", "杨秀英", "王敏", "李强", "张丽", "王勇",
    "刘伟", "张敏", "李静", "王强", "李磊", "王磊", "刘燕", "张军", "李军", "王杰",
    "张杰", "李杰", "王伟", "李伟", "刘强", "张强", "王芳", "李芳", "刘芳", "张建华",
    "李建华", "王建华", "赵云", "钱学森", "孙悟空", "猪八戒", "沙和尚", "唐僧", "贾宝玉", "林黛玉",
    "薛宝钗", "王熙凤", "史湘云", "秦可卿", "妙玉", "迎春", "探春", "惜春", "李纨", "巧姐"
]

CONCEPTS = [
    "知识图谱", "人工智能", "深度学习", "机器学习", "自然语言处理", "神经网络", "大语言模型", "Prompt工程",
    "Java", "Spring Boot", "Spring Cloud", "Microservices", "Docker", "Kubernetes", "DevOps",
    "React", "Vue.js", "Angular", "TypeScript", "JavaScript", "HTML5", "CSS3", "Frontend",
    "Database", "MySQL", "PostgreSQL", "Redis", "MongoDB", "Elasticsearch", "Neo4j",
    "System Design", "Architecture", "Design Patterns", "Algorithms", "Data Structures",
    "Cloud Computing", "AWS", "Azure", "GCP", "Serverless",
    "Security", "OAuth2", "JWT", "Encryption", "Blockchain",
    "Productivity", "Time Management", "GTD", "Pomodoro", "Note Taking"
]

TITLES = [
    "深入理解{}", "{}入门指南", "{}最佳实践", "{}实战演练", "{}核心原理解密",
    "{}面试题解析", "{}源码分析", "{}学习笔记", "{}从入门到精通", "{}常见问题汇总",
    "基于{}的系统设计", "{}性能优化", "{}未来趋势", "{}与{}的结合", "{}开发环境搭建"
]

TAGS = ["学习", "工作", "生活", "技术", "笔记", "感悟", "归档", "重要", "待办", "Review"]

# Helper Functions
def get_random_time():
    start_date = datetime.datetime.now() - datetime.timedelta(days=365)
    random_days = random.randint(0, 365)
    return (start_date + datetime.timedelta(days=random_days)).strftime('%Y-%m-%d %H:%M:%S')

def escape_sql(s):
    return s.replace("'", "''")

def escape_cypher(s):
    return s.replace("'", "\\'")

# Main Generation
def generate_data():
    print(f"Generating data for {NUM_USERS} users, {NUM_NOTES_PER_USER * NUM_USERS} notes...")

    users = []
    notes = []
    content_items = []
    concepts = []

    # 1. Generate Users
    print("Generating Users...")
    for i in range(1, NUM_USERS + 1):
        username = USERNAMES[i-1] if i-1 < len(USERNAMES) else f"User_{i}"
        user = {
            "id": i,
            "username": f"{username}_{i}", # Ensure unique
            "password": "$2a$10$xXTqZTkm/MFK6TItyoM8MOIJj4BuK0L1JPxqky8srasPuXg1nd3Qa",    # Password: 123456 (Verified)
            "email": f"user{i}@example.com",
            "avatar": "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png",
            "status": 1,
            "created_time": get_random_time()
        }
        users.append(user)

    # 2. Generate Content Categories (Optional explicit categories, mostly used in ContentItem)
    
    # 3. Generate Content Items
    print("Generating Content Items...")
    for i in range(1, NUM_CONTENT_ITEMS + 1):
        concept = random.choice(CONCEPTS)
        template = random.choice(TITLES)
        title = template.format(concept, random.choice(CONCEPTS) if template.count("{}") > 1 else "")
        user = random.choice(users)
        
        item = {
            "id": i,
            "source_id": random.randint(1, 100), # Mock source
            "user_id": user["id"],
            "title": title,
            "url": f"https://example.com/article/{i}",
            "author": f"Author_{random.randint(1, 100)}",
            "summary": f"这是一个关于{title}的摘要说明，内容非常精彩。",
            "content": f"这是{title}的详细内容...\n\n这里包含了很多关于{concept}的知识。",
            "status": 1,
            "created_time": get_random_time()
        }
        content_items.append(item)

    # 4. Generate Notes
    print("Generating Notes...")
    note_id_counter = 1
    for user in users:
        for _ in range(NUM_NOTES_PER_USER):
            concept = random.choice(CONCEPTS)
            template = random.choice(TITLES)
            title = template.format(concept, "")
            
            note = {
                "id": note_id_counter,
                "user_id": user["id"],
                "folder_id": 0, # Root
                "title": title,
                "content_md": f"# {title}\n\n这里是关于 **{concept}** 的笔记内容。\n- 重点1\n- 重点2",
                "summary": f"关于{concept}的笔记",
                "tags": ",".join(random.sample(TAGS, 2)),
                "type": "NOTE",
                "status": 2, # Published
                "is_public": random.randint(0, 1),
                "created_time": get_random_time(),
                # For Graph
                "related_concepts": random.sample(CONCEPTS, random.randint(1, 3))
            }
            notes.append(note)
            note_id_counter += 1

    # 5. Generate Concepts (Neo4j)
    # We use global list CONCEPTS
    
    # --- Output MySQL ---
    print(f"Writing {OUTPUT_SQL}...")
    with open(OUTPUT_SQL, "w", encoding="utf-8") as f:
        f.write("-- Mock Data for KnowledgeFlow MySQL\n")
        f.write("SET FOREIGN_KEY_CHECKS=0;\n\n")

        # Users
        f.write("-- Users\n")
        f.write("TRUNCATE TABLE sys_user;\n")
        for u in users:
            sql = f"INSERT INTO sys_user (id, username, password, email, avatar, status, created_time, updated_time, deleted) VALUES ({u['id']}, '{u['username']}', '{u['password']}', '{u['email']}', '{u['avatar']}', {u['status']}, '{u['created_time']}', '{u['created_time']}', 0);\n"
            f.write(sql)
        f.write("\n")

        # Content Items
        f.write("-- Content Items\n")
        f.write("TRUNCATE TABLE content_item;\n")
        for c in content_items:
            sql = f"INSERT INTO content_item (id, source_id, user_id, title, url, author, summary, content_text, status, created_time, deleted) VALUES ({c['id']}, {c['source_id']}, {c['user_id']}, '{c['title']}', '{c['url']}', '{c['author']}', '{c['summary']}', '{c['content']}', {c['status']}, '{c['created_time']}', 0);\n"
            f.write(sql)
        f.write("\n")

        # Notes
        f.write("-- Notes\n")
        f.write("TRUNCATE TABLE note_entry;\n")
        for n in notes:
            sql = f"INSERT INTO note_entry (id, user_id, folder_id, title, content_md, summary, tags, type, status, is_public, created_time, updated_time, deleted) VALUES ({n['id']}, {n['user_id']}, {n['folder_id']}, '{n['title']}', '{n['content_md']}', '{n['summary']}', '{n['tags']}', '{n['type']}', {n['status']}, {n['is_public']}, '{n['created_time']}', '{n['created_time']}', 0);\n"
            f.write(sql)
        
        f.write("\nSET FOREIGN_KEY_CHECKS=1;\n")

    # --- Output Neo4j ---
    print(f"Writing {OUTPUT_CYPHER}...")
    with open(OUTPUT_CYPHER, "w", encoding="utf-8") as f:
        f.write("// Mock Data for KnowledgeFlow Neo4j\n")
        f.write("// Clear existing data\n")
        f.write("MATCH (n) DETACH DELETE n;\n\n")

        # Create Concepts
        f.write("// Concepts\n")
        for c in CONCEPTS:
            desc = f"Concept definition for {c}"
            cypher = f"MERGE (c:Concept {{name: '{c}'}}) SET c.description = '{desc}';\n"
            f.write(cypher)
        f.write("\n")

        # Create Users (UserNode)
        f.write("// Users\n")
        for u in users:
            cypher = f"create (u:User {{userId: {u['id']}, username: '{u['username']}'}});\n"
            f.write(cypher)
        f.write("\n")

        # Create Notes and Relationships
        f.write("// Notes and Relations\n")
        for n in notes:
            # Create Note Node
            cypher = f"CREATE (n:Note {{noteId: {n['id']}, title: '{n['title']}', summary: '{n['summary']}'}});\n"
            f.write(cypher)
            
            # Link to User (CREATED relationship - inferred)
            # Even if UserNode.java didn't show it, it's good practice for personalized graph
            cypher = f"MATCH (u:User {{userId: {n['user_id']}}}), (n:Note {{noteId: {n['id']}}}) CREATE (u)-[:CREATED]->(n);\n"
            f.write(cypher)

            # Link to Concepts (MENTIONS)
            for c_name in n['related_concepts']:
                 cypher = f"MATCH (n:Note {{noteId: {n['id']}}}), (c:Concept {{name: '{c_name}'}}) MERGE (n)-[:MENTIONS]->(c);\n"
                 f.write(cypher)
            
            # Tags
            note_tags = n['tags'].split(",")
            for tag in note_tags:
                cypher = f"MERGE (t:Tag {{name: '{tag}'}});\n"
                f.write(cypher)
                cypher = f"MATCH (n:Note {{noteId: {n['id']}}}), (t:Tag {{name: '{tag}'}}) MERGE (n)-[:TAGGED_WITH]->(t);\n"
                f.write(cypher)
            
            f.write("\n")

        # Create Concept-Concept Relationships (Random)
        f.write("// Concept Relations\n")
        for _ in range(100):
            c1 = random.choice(CONCEPTS)
            c2 = random.choice(CONCEPTS)
            if c1 != c2:
                cypher = f"MATCH (a:Concept {{name: '{c1}'}}), (b:Concept {{name: '{c2}'}}) MERGE (a)-[:RELATED_TO]->(b);\n"
                f.write(cypher)

    print("Done! Files generated:")
    print(f"- {OUTPUT_SQL}")
    print(f"- {OUTPUT_CYPHER}")

if __name__ == "__main__":
    generate_data()
