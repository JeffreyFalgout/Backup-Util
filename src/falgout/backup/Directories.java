package falgout.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class Directories {
    public static final FileVisitor<Object> DO_NOTHING = new SimpleFileVisitor<Object>() {};
    
    private Directories() {}
    
    public static void delete(Path dir) throws IOException {
        delete(dir, DO_NOTHING);
    }
    
    public static void delete(Path dir, final FileVisitor<? super Path> progressMonitor) throws IOException {
        if (Files.notExists(dir)) { return; }
        
        Files.walkFileTree(dir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return progressMonitor.preVisitDirectory(dir, attrs);
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                
                return progressMonitor.visitFile(file, attrs);
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return progressMonitor.visitFileFailed(file, exc);
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                
                return progressMonitor.postVisitDirectory(dir, exc);
            }
        });
    }
    
    public static void delete(Directory dir) throws IOException {
        delete(dir, DO_NOTHING);
    }
    
    public static void delete(Directory dir, FileVisitor<? super Path> progressMonitor) throws IOException {
        delete(dir.getPath(), progressMonitor);
    }
    
    public static void copy(Directory source, Directory target, CopyOption... options) throws IOException {
        copy(source, target, DO_NOTHING, options);
    }
    
    public static void copy(final Directory source, final Directory target,
            final FileVisitor<? super Path> progressMonitor, final CopyOption... options) throws IOException {
        Files.walkFileTree(source.getPath(), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                
                return progressMonitor.preVisitDirectory(dir, attrs);
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), options);
                
                return progressMonitor.visitFile(file, attrs);
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return progressMonitor.visitFileFailed(file, exc);
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return progressMonitor.postVisitDirectory(dir, exc);
            }
        });
    }
    
    public static boolean isStructureSame(Directory d1, Directory d2) throws IOException {
        return isStructureSame(d1, d2, DO_NOTHING);
    }
    
    public static boolean isStructureSame(Directory d1, Directory d2, FileVisitor<? super Path> progressMonitor)
            throws IOException {
        Set<Path> f1 = new TreeSet<>();
        for (Path p : d1) {
            f1.add(d1.relativize(p));
        }
        Set<Path> f2 = new TreeSet<>();
        for (Path p : d2) {
            f2.add(d2.relativize(p));
        }
        
        return f1.equals(f2);
    }
    
    public static byte[] digest(Directory dir, MessageDigest md) throws IOException {
        Set<Path> paths = new TreeSet<>();
        for (Path p : dir) {
            paths.add(dir.resolve(p));
        }
        
        List<InputStream> streams = new ArrayList<>(paths.size());
        for (Path p : paths) {
            if (Files.isRegularFile(p)) {
                streams.add(Files.newInputStream(p));
            }
            md.update(p.toString().getBytes());
        }
        
        return digest(new SequenceInputStream(Collections.enumeration(streams)), md);
    }
    
    public static byte[] digest(InputStream in, MessageDigest md) throws IOException {
        try (InputStream close = in) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = in.read(buf)) > 0) {
                md.update(buf, 0, read);
            }
        }
        
        return md.digest();
    }
}
