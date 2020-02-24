package com.changgou.util;

import com.changgou.file.FastDFSFile;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.springframework.core.io.ClassPathResource;

import java.io.*;

public class FastDFSClient {
    /**
     * 加载Tracker的连接信息
     */
    static {
        try {
        //加载classpath下的文件路径
        String path = new ClassPathResource("fdfs_client.conf").getPath();
        //加载配置路径
            ClientGlobal.init(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建TrackerServer对象
     * @return TrackerServer
     */
    public static TrackerServer getTrackerServer(){
        //定义一个空对象
        TrackerServer trackerServer = null;
        try {
            //创建连接tracker对象
            TrackerClient trackerClient = new TrackerClient();
            //创建连接,返回一个TrackerServer对象
            trackerServer = trackerClient.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trackerServer;
    }

    /**
     * 创建StorageClient对象
     * @return StorageClient
     */
    public static StorageClient getStorageClient(){
        //通过TrackerServer得到所要连接的Storage
        StorageClient storageClient = new StorageClient(getTrackerServer(), null);
        return storageClient;
    }

    /**
     * 文件上传
     * @param fastDFSFile 上传包装对象
     */
    public static String[] upload(FastDFSFile fastDFSFile){
        String[] uploadFile = null;
        try {
        //附加参数
        NameValuePair[] meta_list = new NameValuePair[1];
        //文件作者
        meta_list[0] = new NameValuePair("author",fastDFSFile.getAuthor());
        //上传文件
        //upload_file(文件字节数组,文件扩展名,文件附加参数)
            uploadFile = getStorageClient().upload_file(fastDFSFile.getContent(), fastDFSFile.getExt(),
                    meta_list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uploadFile;
    }

    /**
     * 获取文件信息
     * @param groupName 组名
     * @param remoteFileName 文件存储完整名
     * @return
     */
    public static FileInfo getFileInfo(String groupName,String remoteFileName){
        FileInfo info  = null;
        try {
            //获取文件信息
            info = getStorageClient().get_file_info(groupName, remoteFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    /**
     * 文件下载
     * @param groupName 组名
     * @param remoteFileName 文件存储完整名
     * @return
     */
    public static InputStream downloadFile(String groupName,String remoteFileName){
        InputStream is = null;
        try {
            //下载文件
            byte[] bytes = getStorageClient().download_file(groupName, remoteFileName);
            //把字节数组转换为字节输入流
            is = new ByteArrayInputStream(bytes);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    /**
     * 删除文件
     * @param groupName 组名
     * @param remoteFileName 文件存储完整名
     * @return
     */
    public static void deleteFile(String groupName,String remoteFileName){
        try {
            //删除文件
            getStorageClient().delete_file(groupName, remoteFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取组信息
     * @param groupName 组名
     * @return
     */
    public static StorageServer getStorageServer(String groupName){
        StorageServer storageServer = null;
        try {
            //创建一个TrakerClient对象
            TrackerClient trackerClient = new TrackerClient();
            //建立连接对象
            TrackerServer trackerServer = trackerClient.getConnection();
            //通过连接对象和组名得到指定存储对象
            storageServer = trackerClient.getStoreStorage(trackerServer, groupName);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return storageServer;
    }

    /**
     * 根据文件组名跟文件存储路径获取storage服务的ip、端口信息
     * @param groupName 组名
     * @param remoteFileName 文件存储完整名
     * @return
     */
    public static ServerInfo[] getServerInfo(String groupName,String remoteFileName){
        ServerInfo[] info = null;
        try {
            //创建TrakerClient对象
            TrackerClient trackerClient = new TrackerClient();
            //创建连接对象
            TrackerServer trackerServer = trackerClient.getConnection();
            //根据组名文件路径获得组信息对象
            info = trackerClient.getFetchStorages(trackerServer, groupName, remoteFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    /**
     * 获得Tracker服务地址
     * @return
     */
    public static String getTrackerUrl(){
        //调用静态方法获得连接对象
        TrackerServer trackerServer = getTrackerServer();
        //拼接地址
        String url = "http://"+trackerServer.getInetSocketAddress().getHostString()+":"
                +ClientGlobal.getG_tracker_http_port()+"/";
        return url;
    }

    public static void main(String[] args) {
        //获取文件信息测试
//        FileInfo info = getFileInfo("group1", "M00/00/00/wKjThF46jvSAVdB-AAAFqRELXeY471.txt");
//        System.out.println(info.getCreateTimestamp());
//        System.out.println(info.getCrc32());
//        System.out.println(info.getFileSize());
//        System.out.println(info.getSourceIpAddr());


        //文件下载测试
//        InputStream is = downloadFile("group1", "M00/00/00/wKjThF46jvSAVdB-AAAFqRELXeY471.txt");
//        try {
//            //定义输出流对象
//            OutputStream os = new FileOutputStream("D:/1.txt");
//            //定义缓冲区
//            byte[] buff = new byte[1024];
//            //读取输入流
//            while (is.read(buff)>-1){
//                os.write(buff);
//            }
//            os.close();
//            is.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //获取组信息测试
//        StorageServer group1 = getStorageServer("group1");
//        System.out.println("storage下标:"+group1.getStorePathIndex());
//        System.out.println("storge的ip与端口(带斜线):"+group1.getInetSocketAddress());

        //根据组名跟文件路径获得多组信息测试
//        ServerInfo[] info = getServerInfo("group1", "M00/00/00/wKjThF46jvSAVdB-AAAFqRELXeY471.txt");
//        for (ServerInfo serverInfo : info) {
//            System.out.println("组ip:"+serverInfo.getIpAddr());
//            System.out.println("组端口:"+serverInfo.getPort());
//    }

        System.out.println(ClientGlobal.getG_tracker_http_port());
    }
}
