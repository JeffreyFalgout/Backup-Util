package falgout.backup.app;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import falgout.backup.FileStoreLocator;

@RunWith(MockitoJUnitRunner.class)
public class BackupConfigurationTest {
	private static final Path relSrc = Paths.get("src");
	private static final Path relSrcFalgout = relSrc.resolve("falgout");
	private static final Path bin = Paths.get("bin");
	private static final Path cd = Paths.get("/home/jeffrey/git/Backup-Util");
	
	@Mock private FileStore store;
	@Mock private FileStoreLocator locator;
	private BackupConfiguration conf;
	private Path src;
	private Path srcFalgout;
	
	@Before
	public void init() throws IOException {
		when(locator.getRootLocation(store)).thenReturn(cd);
		conf = BackupConfiguration.load(store, locator);
		
		src = relSrc;
		srcFalgout = relSrcFalgout;
	}
	
	@Test
	public void AddingDirectoriesKeepsHighestLevel() throws IOException {
		conf.addDirectory(src);
		assertEquals(Collections.singleton(relSrc), conf.getDirectoriesToBackup());
		conf.addDirectory(srcFalgout);
		assertEquals(Collections.singleton(relSrc), conf.getDirectoriesToBackup());
	}
	
	@Test
	public void AddingDirectoriesRemovedLowerLevel() throws IOException {
		conf.addDirectory(srcFalgout);
		assertEquals(Collections.singleton(relSrcFalgout), conf.getDirectoriesToBackup());
		conf.addDirectory(src);
		assertEquals(Collections.singleton(relSrc), conf.getDirectoriesToBackup());
	}
	
	@Test
	public void AddingAbsoluteDirectoriesWorks() throws IOException {
		src = src.toRealPath();
		srcFalgout = srcFalgout.toRealPath();
		
		AddingDirectoriesKeepsHighestLevel();
		conf.removeDirectory(relSrc);
		AddingDirectoriesRemovedLowerLevel();
	}
	
	@Test(expected = IOException.class)
	public void ErrorIfAddingDirectoryThatDoesntExist() throws IOException {
		conf.addDirectory(Paths.get("ergkjahlkrg"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void ErrorIfAddingDisjointDirectory() throws IOException {
		conf.addDirectory(Paths.get("/proc"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void ErrorIfAddingFile() throws IOException {
		conf.addDirectory(Paths.get("LICENSE"));
	}
	
	@Test
	public void SaveAndReload() throws IOException {
		conf.addDirectory(src);
		conf.addDirectory(bin);
		conf.save();
		BackupConfiguration conf2 = BackupConfiguration.load(store, locator);
		assertEquals(conf, conf2);
		assertEquals(conf.getDirectoriesToBackup(), conf2.getDirectoriesToBackup());
		conf.delete();
	}
}
