import pandas as pd
import matplotlib.pyplot as plt

# Đọc dữ liệu từ kết quả thực nghiệm Java
df = pd.read_csv("cutoff_results.csv")

# 1. Biểu đồ chi tiết trên tập dữ liệu RANDOM (kèm dải độ lệch chuẩn +-1 StdDev)
rand_df = df[df["DataType"] == "RANDOM"]
best_row = rand_df.loc[rand_df["MeanTimeMs"].idxmin()]
best_cutoff = int(best_row["Cutoff"])
best_time = best_row["MeanTimeMs"]

plt.figure(figsize=(10, 5.5), dpi=300)
plt.plot(rand_df["Cutoff"], rand_df["MeanTimeMs"], marker="o", linewidth=2,
         color="#1f77b4", label="Thời gian trung bình (Mean Time)")
plt.fill_between(rand_df["Cutoff"],
                 rand_df["MeanTimeMs"] - rand_df["StdDevMs"],
                 rand_df["MeanTimeMs"] + rand_df["StdDevMs"],
                 color="#1f77b4", alpha=0.2, label="Độ lệch chuẩn (±1 StdDev)")

# Đánh dấu CUTOFF = 7 (Giáo trình cũ), 10 (Giáo trình mới) và Tối ưu thực nghiệm
plt.axvline(x=7, color="#ff7f0e", linestyle="--", linewidth=1.5, label="CUTOFF = 7 (Giáo trình cũ)")
plt.axvline(x=10, color="#2ca02c", linestyle="--", linewidth=1.5, label="CUTOFF = 10 (Giáo trình mới)")
plt.scatter([best_cutoff], [best_time], color="#d62728", s=90, zorder=5,
            label=f"Tối ưu thực nghiệm: CUTOFF = {best_cutoff} ({best_time:.2f} ms)")

plt.title("Ảnh hưởng của ngưỡng CUTOFF đến thời gian chạy Mergesort (N = 1,000,000 - Random)", fontsize=13, fontweight="bold")
plt.xlabel("Giá trị CUTOFF (Độ dài mảng con chuyển sang Insertion Sort)", fontsize=11)
plt.ylabel("Thời gian thực thi trung bình (ms)", fontsize=11)
plt.xticks( [1, 4, 7, 10, 12, 16, 20, 24, 32, 40, 48, 64])
plt.grid(True, linestyle=":", alpha=0.6)
plt.legend(loc="upper right")
plt.tight_layout()
plt.savefig("cutoff_random_detail.png")
plt.close()

# 2. Biểu đồ so sánh 4 phân phối dữ liệu
plt.figure(figsize=(10, 5.5), dpi=300)
for dtype, group in df.groupby("DataType"):
    plt.plot(group["Cutoff"], group["SpeedupPct"], marker="s", linewidth=1.8, label=dtype)

plt.axvline(x=7, color="gray", linestyle="--", alpha=0.7, label="CUTOFF = 7")
plt.axvline(x=10, color="black", linestyle=":", alpha=0.7, label="CUTOFF = 10")
plt.axhline(y=0, color="red", linestyle="-", linewidth=0.8, alpha=0.5)

plt.title("Tỷ lệ cải thiện tốc độ (Speedup %) theo CUTOFF trên 4 phân phối dữ liệu", fontsize=13, fontweight="bold")
plt.xlabel("Giá trị CUTOFF", fontsize=11)
plt.ylabel("Mức độ cải thiện tốc độ so với CUTOFF = 1 (%)", fontsize=11)
plt.xticks([1, 4, 7, 10, 12, 16, 20, 24, 32, 40, 48, 64])
plt.grid(True, linestyle=":", alpha=0.6)
plt.legend()
plt.tight_layout()
plt.savefig("cutoff_all_distributions.png")
plt.close()

print("[+] Đã xuất 2 biểu đồ: cutoff_random_detail.png và cutoff_all_distributions.png")