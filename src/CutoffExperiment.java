import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Chương trình thực nghiệm khảo sát giá trị CUTOFF tối ưu cho thuật toán Mergesort.
 * Thiết kế theo phương pháp phân tích thực nghiệm và thống kê của giáo trình:
 * Robert Sedgewick & Kevin Wayne, "Algorithms" (4th Edition),
 * Chương 1.4 (Analysis of Algorithms - Stopwatch / Doubling & Empirical Analysis)
 * và Chương 2.2 (Mergesort - Cutoff to Insertion Sort).
 */
public class CutoffExperiment {

    // Cấu hình thí nghiệm
    private static final int N = 1_000_000;          // Kích thước mảng thực nghiệm (1 triệu phần tử)
    private static final int TRIALS = 25;            // Số phép thử lặp độc lập (T = 25 trials/mốc)
    private static final long BASE_SEED = 20261110L; // Hạt giống cố định đảm bảo tính tái lập (Reproducibility)

    // Danh sách các giá trị CUTOFF khảo sát (từ 1 đến 64)
    private static final int[] CUTOFF_VALUES = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 18, 20, 24, 28, 32, 40, 48, 64
    };

    public enum DataType {
        RANDOM, NEARLY_SORTED, REVERSE_SORTED, FEW_UNIQUE
    }

    /**
     * Sinh mảng dữ liệu theo kịch bản phân phối và hạt giống cố định (theo chuẩn StdRandom của Sedgewick).
     */
    public static int[] generateDataset(int n, DataType type, long seed) {
        Random rand = new Random(seed);
        int[] a = new int[n];

        switch (type) {
            case RANDOM:
                // Phân phối ngẫu nhiên đều (Knuth / Fisher-Yates / uniform 32-bit int)
                for (int i = 0; i < n; i++) {
                    a[i] = rand.nextInt();
                }
                break;

            case NEARLY_SORTED:
                // Mảng tăng dần có 2% số cặp vị trí bị hoán đổi ngẫu nhiên
                for (int i = 0; i < n; i++) {
                    a[i] = i;
                }
                int swaps = Math.max(1, n / 50);
                for (int k = 0; k < swaps; k++) {
                    int i = rand.nextInt(n);
                    int j = rand.nextInt(n);
                    int swap = a[i];
                    a[i] = a[j];
                    a[j] = swap;
                }
                break;

            case REVERSE_SORTED:
                // Mảng giảm dần nghiêm ngặt N, N-1, ..., 1 (Worst Case của Insertion Sort)
                for (int i = 0; i < n; i++) {
                    a[i] = n - i;
                }
                break;

            case FEW_UNIQUE:
                // Mảng ít khóa phân biệt: chỉ chứa các giá trị thuộc tập {0, 1, ..., 9}
                for (int i = 0; i < n; i++) {
                    a[i] = rand.nextInt(10);
                }
                break;
        }
        return a;
    }

    /**
     * Xuất 4 bộ dữ liệu mẫu ra thư mục datasets/ theo định dạng chuẩn algs4 (dòng đầu là N, tiếp theo là N phần tử).
     */
    public static void exportSampleDatasets(int sampleSize) throws IOException {
        File dir = new File("datasets");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (DataType type : DataType.values()) {
            int[] data = generateDataset(sampleSize, type, BASE_SEED);
            File file = new File(dir, "dataset_" + type.name().toLowerCase(Locale.ROOT) + "_" + sampleSize + ".txt");
            try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                out.println(sampleSize);
                for (int val : data) {
                    out.println(val);
                }
            }
        }
        System.out.println("[+] Đã xuất 4 file dữ liệu mẫu vào thư mục ./datasets/");
    }

    /**
     * Tính trung bình mẫu (Sample Mean) theo StdStats.mean() của Sedgewick & Wayne.
     */
    public static double mean(double[] x) {
        if (x.length == 0) return Double.NaN;
        double sum = 0.0;
        for (double v : x) {
            sum += v;
        }
        return sum / x.length;
    }

    /**
     * Tính độ lệch chuẩn mẫu (Sample Standard Deviation) theo StdStats.stddev() của Sedgewick & Wayne.
     */
    public static double stddev(double[] x) {
        if (x.length <= 1) return 0.0;
        double m = mean(x);
        double sumSq = 0.0;
        for (double v : x) {
            sumSq += (v - m) * (v - m);
        }
        return Math.sqrt(sumSq / (x.length - 1));
    }

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        System.out.println("=== BẮT ĐẦU THÍ NGHIỆM KHẢO SÁT CUTOFF CHO MERGESORT (CHUẨN SEDGEWICK & WAYNE) ===");
        System.out.println("Cấu hình: N = " + N + ", Số lần lặp (Trials) = " + TRIALS);
        System.out.println("OS: " + System.getProperty("os.name") + " | Java: " + System.getProperty("java.version"));

        // 1. Xuất bộ dữ liệu test mẫu ra thư mục datasets/ (10,000 phần tử/file)
        exportSampleDatasets(10_000);

        // 2. Làm nóng JVM (JIT Warm-up: 6 vòng với N = 200,000) để C2 JIT Compiler tối ưu hóa mã máy
        System.out.print("[*] Đang chạy JVM Warm-up...");
        for (int w = 0; w < 6; w++) {
            int[] warmArr = generateDataset(200_000, DataType.RANDOM, BASE_SEED + w);
            MergeSort.sort(warmArr, 7);
        }
        System.out.println(" Hoàn tất!\n");

        // 3. Chạy thực nghiệm và ghi kết quả ra cutoff_results.csv
        try (PrintWriter csv = new PrintWriter(new FileWriter("cutoff_results.csv"))) {
            csv.println("DataType,N,Cutoff,MeanTimeMs,StdDevMs,MinTimeMs,MaxTimeMs,SpeedupPct");

            for (DataType type : DataType.values()) {
                System.out.println(">>> Phân phối dữ liệu: " + type.name());
                System.out.printf("%-8s | %-12s | %-12s | %-10s | %-10s | %-10s%n",
                        "CUTOFF", "Mean (ms)", "StdDev (ms)", "Min (ms)", "Max (ms)", "Cải thiện");
                System.out.println("---------------------------------------------------------------------------");

                // Chuẩn bị trước mảng gốc của T = 25 trials ngoài đoạn bấm giờ
                int[][] baseArrays = new int[TRIALS][];
                for (int t = 0; t < TRIALS; t++) {
                    baseArrays[t] = generateDataset(N, type, BASE_SEED + t);
                }

                double baselineMean = 0.0;
                int bestCutoff = 1;
                double bestMean = Double.MAX_VALUE;

                for (int cutoff : CUTOFF_VALUES) {
                    System.gc(); // Dọn rác một lần trước mỗi mốc CUTOFF để tránh nhiễu GC giữa các trials
                    double[] timesMs = new double[TRIALS];

                    for (int t = 0; t < TRIALS; t++) {
                        int[] workArr = Arrays.copyOf(baseArrays[t], N);

                        // Đo thời gian chạy theo chuẩn Stopwatch (System.nanoTime)
                        long startNano = System.nanoTime();
                        MergeSort.sort(workArr, cutoff);
                        long endNano = System.nanoTime();

                        if (!MergeSort.isSorted(workArr)) {
                            throw new IllegalStateException("Lỗi: Mảng chưa được sắp xếp đúng!");
                        }
                        timesMs[t] = (endNano - startNano) / 1_000_000.0;
                    }

                    double mu = mean(timesMs);
                    double sigma = stddev(timesMs);
                    double minT = Arrays.stream(timesMs).min().orElse(0.0);
                    double maxT = Arrays.stream(timesMs).max().orElse(0.0);

                    if (cutoff == 1) {
                        baselineMean = mu;
                    }
                    double speedup = ((baselineMean - mu) / baselineMean) * 100.0;

                    if (mu < bestMean) {
                        bestMean = mu;
                        bestCutoff = cutoff;
                    }

                    System.out.printf("%-8d | %-12.3f | %-12.3f | %-10.3f | %-10.3f | %+8.2f%%%n",
                            cutoff, mu, sigma, minT, maxT, speedup);

                    csv.printf("%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                            type.name(), N, cutoff, mu, sigma, minT, maxT, speedup);
                }
                System.out.printf("==> [KẾT LUẬN %s]: CUTOFF tốt nhất = %d (Mean = %.3f ms, nhanh hơn %.2f%% so với CUTOFF = 1)%n%n",
                        type.name(), bestCutoff, bestMean, ((baselineMean - bestMean) / baselineMean) * 100.0);
            }
        }
        System.out.println("[+] Đã lưu toàn bộ số liệu thống kê vào file: cutoff_results.csv");
    }
}
