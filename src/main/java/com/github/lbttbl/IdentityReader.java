package com.github.lbttbl;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.RETR_CCOMP;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class IdentityReader {

    public String read(String filePath) {

        Mat srcMat = Imgcodecs.imread(filePath);
        Mat distMat = new Mat();

        // 转灰度
        Imgproc.cvtColor(srcMat, distMat, COLOR_BGR2GRAY);
        // 降噪
        Imgproc.blur(distMat, distMat, new Size(3, 3));
        // 二值化图，主要将灰色部分转成白色，使内容为黑色
        Imgproc.threshold(distMat, distMat, 90, 255, THRESH_BINARY);
        // 膨胀
        Imgproc.dilate(distMat, distMat, new Mat());
        Imgcodecs.imwrite("dilate.png", distMat);
        // 腐蚀，使黑色的文字部分连接到一起
        Imgproc.erode(distMat, distMat, new Mat(3, 10, CvType.CV_8U), new Point(-1, -1), 5);
        Imgcodecs.imwrite("erode.png", distMat);

        // 查找轮廓。
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(distMat, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE);

        // 找到身份证号的轮廓，并生成新图片
        return findNumber(srcMat, contours);
    }

    private String doOCR(String filePath) {
        File imageFile = new File(filePath);
        ITesseract instance = new Tesseract();
        //设置tessdata的路径，如果放在项目根目录下则不需要制定，如果不在则指定为tessdata目录的绝对路径
        instance.setDatapath(IdentityReader.class.getResource("/").getPath());
        //如果是识别英文，不需要以下语句指定，如果识别英文以外的语言，则需要设置识别语言（中文是chi_sim）
        instance.setLanguage("eng");
        try {
            return instance.doOCR(imageFile);
        } catch (TesseractException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String findNumber(Mat srcMat, List<MatOfPoint> contours) {
        for (int i = 0; i <contours.size(); i++) {
            Rect rect = Imgproc.boundingRect(contours.get(i));
            if(rect.width / rect.height > 6 && rect.width / rect.height < 20 && rect.width > srcMat.width() / 4) {
                Mat targetMat = new Mat(srcMat, rect);
                Imgcodecs.imwrite("result.png", targetMat);

                String ocrResult = doOCR("result.png");
                if(Pattern.matches("\\d+", ocrResult.trim())) {
                    return ocrResult;
                } else {
                    System.out.println("非正确答案：" + ocrResult);
                }

            }
        }
        return null;
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        IdentityReader reader = new IdentityReader();
        String number = reader.read(args[0]);
        if(number == null) {
            System.out.println("识别失败");
        } else {
            System.out.println(number);
        }
    }
}
