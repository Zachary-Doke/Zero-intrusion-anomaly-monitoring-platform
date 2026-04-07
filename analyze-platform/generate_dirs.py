import os

base_dir = "platform-server"

dirs = [
    "src/main/java/com/platform/analyze/entity",
    "src/main/java/com/platform/analyze/dto",
    "src/main/java/com/platform/analyze/repository",
    "src/main/java/com/platform/analyze/service",
    "src/main/java/com/platform/analyze/controller",
    "src/main/java/com/platform/analyze/config",
    "src/main/java/com/platform/analyze/ai",
    "src/main/java/com/platform/analyze/aggregate",
    "src/main/java/com/platform/analyze/common",
    "src/main/resources"
]

for d in dirs:
    os.makedirs(os.path.join(base_dir, d), exist_ok=True)

print("Directories created successfully.")
