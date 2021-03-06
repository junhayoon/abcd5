package kr.co.ismartcity.smartfactory.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.co.ismartcity.smartfactory.entity.Facility;
import kr.co.ismartcity.smartfactory.entity.FacilityCategory;
import kr.co.ismartcity.smartfactory.entity.QFacility;
import kr.co.ismartcity.smartfactory.entity.QFacilityCategory;
import kr.co.ismartcity.smartfactory.model.FacilityStatus;
import kr.co.ismartcity.smartfactory.repository.FacilityCategoryRepository;
import kr.co.ismartcity.smartfactory.repository.FacilityRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConfigurationProperties
public class FacilityService {

	final static public String defaultSymbolPath 			= "static/symbol";
	final static public String defaultFacilityStatusPath 	= "static/symbol/facility";
	
	@Value("${facility.filepath.symbol}")
	public String symbolUploadPath;
	
	@Autowired
	public FacilityCategoryRepository facilityCategoryRepository;
	
	@Autowired
	public FacilityRepository facilityRepository;
	
	@Autowired
	public JPAQueryFactory jpaQueryFactory;
	
	private void checkUploadFolder()
	{
		try
		{
			File fUploadFolder = new File(symbolUploadPath);
			if (!fUploadFolder.exists())
				fUploadFolder.mkdirs();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
		
	public List<String> getStaticSymbolList() {
		
		List<String> symbolList = new ArrayList<String>();
		
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		try {
			Resource[] resources = resolver.getResources("classpath:" + defaultSymbolPath + "/*.png");
			for (Resource resource : resources) {
				symbolList.add(resource.getFilename());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return symbolList;
	}
	
	public List<String> getUploadSymbolList() {
		
		List<String> symbolList = new ArrayList<String>();
		
		String fulePath = symbolUploadPath;
		if (fulePath != null && fulePath.length() > 0 && fulePath.charAt(fulePath.length() - 1) == '/') {
			fulePath = fulePath.substring(0, fulePath.length() - 1);
	    }
		File dirFile = new File(fulePath);
		
		File[] fileList = dirFile.listFiles();
		
		if(fileList != null) {
			for(File file: fileList) {             
				symbolList.add(file.getName());
			}
		}
		
		return symbolList;
	}
	
	public Resource loadFileAsResource(String fileName, int removable) {
		if(removable == 0) {				
			ClassPathResource pathResource = new ClassPathResource(defaultSymbolPath + "/" + fileName);
			
			try {
				Resource r = new UrlResource(pathResource.getURI());
				return r;
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Path uploadPath = Paths.get(symbolUploadPath).toAbsolutePath().normalize();
			Path filePath = uploadPath.resolve(fileName).normalize();
			try {
				Resource resource = new UrlResource(filePath.toUri());
				return resource;
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			}
		}
        
		return null;
	}
	
	public String saveFile(MultipartFile file) {
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		try {
			checkUploadFolder();
            if(!fileName.contains("..")) {
            	Path uploadPath = Paths.get(symbolUploadPath).toAbsolutePath().normalize();
            	
            	int pos = fileName.lastIndexOf(".");
            	String _fileName = fileName.substring(0, pos);
            	String _fileExt = fileName.substring(pos + 1);
            	
            	DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            	String timeString = timeStampPattern.format(LocalDateTime.now());
            	
            	fileName = _fileName + "_" + timeString + "." + _fileExt;
            		
    			Path filePath = uploadPath.resolve(fileName).normalize();
    			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            }            

            return fileName;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		return null;
	}
	
	public String deleteFile(String filename) {
		try {
			int pos = filename.lastIndexOf(".");
			if(pos == -1) {
				filename += ".png";
			}
			Path uploadPath = Paths.get(symbolUploadPath).toAbsolutePath().normalize();
			Path filePath = uploadPath.resolve(filename).normalize();
			Files.delete(filePath);
			
			return filename;
		} catch (IOException ex) {
            ex.printStackTrace();
        }
		
		return null;
	}
	
	public Resource loadFacilityStatusFileAsResource(String fileName) {						
		Resource r = null;
		
		ClassPathResource pathResource = new ClassPathResource(defaultFacilityStatusPath + "/" + fileName);
		
		try {
			r = new UrlResource(pathResource.getURI());
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return r;
	}
	
	public List<FacilityCategory> getAllCategorys() {
		return (List<FacilityCategory>)facilityCategoryRepository.findAll();
	}
	
	public List<FacilityCategory> getCategorysByEnabled(boolean enabled) {
		return (List<FacilityCategory>)facilityCategoryRepository.findAll(QFacilityCategory.facilityCategory.enabled.eq(enabled));
	}
	
	public void addCategory(FacilityCategory facilityCategory) {
		facilityCategoryRepository.save(facilityCategory);
	}
	
	public void updateCategory(FacilityCategory facilityCategory) {
		//log.debug(String.format("updateCategory() 1... facilityCategory(%s)", facilityCategory));
		
		facilityCategoryRepository.save(facilityCategory);
	}
	
	public int removeCategory(String categoryCcode) {
		Optional<FacilityCategory> optional = facilityCategoryRepository.findById(categoryCcode);
		if(optional.isPresent()) {
			List<Facility> facilitys = (List<Facility>) facilityRepository.findAll(QFacility.facility.facilityCategory.eq(optional.get()));
			if(facilitys != null && facilitys.size() > 0) {
				return -2;
			}
			
			try {
				facilityCategoryRepository.delete(optional.get());
				return 0;
			} catch(Exception e) {
				return -3;
			}
		}
		
		return -1;
	}
	
	public int addFacility(Facility facility) {
		log.debug(String.format("addFacility() 1... facility.getProperties(%s)", facility.getProperties()));
		
		int nRet = 0;
		if (facility != null && facility.getProperties() != null &&
			facility.getProperties().get("mediaChannel") != null)
		{
			List<Facility> facilitys = facilityRepository.findFacilityByProperty("mediaChannel", facility.getProperties().get("mediaChannel"));
			if (facilitys.size() > 0)
				nRet = -1; 		// channel name is duplicated.
		}
		Facility entity = facilityRepository.save(facility);
		System.out.println(entity.getFacilityCategory().getCcode());
		System.out.println(entity.getFcode());
		/*
		 * if (nRet == 0) { Facility entity = facilityRepository.save(facility);
		 * if(entity.getFacilityCode() < 0) { nRet = -2; // this error is occurred by
		 * database. } else { // should implement codes that connect cctv to vms later
		 * nRet = 0; // success } }
		 */
		
		return nRet;
	}
	
	public int updateFacility(Facility facility) {
		int nRet = 0;
		String newChannel = (String) facility.getProperties().get("mediaChannel");
		String oldChannel = "";
		Facility oldFacility = null;
		Optional<Facility> optional = facilityRepository.findById(facility.getFcode());
		if(optional.isPresent()) {
			oldChannel = (String) optional.get().getProperties().get("mediaChannel");
			oldFacility = optional.get();
		}
		
		if(newChannel != null && newChannel.length() > 0) {
			List<Facility> facilitys = facilityRepository.findFacilityByProperty("mediaChannel", newChannel);
			if(facilitys.size() > 1) nRet = -1; 		// channel name is duplicated.
			else if(facilitys.size() == 1) {
				if(facilitys.get(0).getFcode() != facility.getFcode()) nRet = -1;
			}
		}
		Facility entity = facilityRepository.save(facility);
		/*
		 * if (nRet == 0) { Facility entity = facilityRepository.save(facility);
		 * if(entity.getFacilityCode() < 0) { nRet = -2; } else { // should implement
		 * codes that connect cctv to vms later nRet = 0; // success } }
		 */

		return nRet;
	}	
	
	public int updateFacility_old(Facility facility) {
		String newChannel = (String) facility.getProperties().get("mediaChannel");
		String oldChannel = "";
		Facility oldFacility = null;
		Optional<Facility> optional = facilityRepository.findById(facility.getFcode());
		if(optional.isPresent()) {
			oldChannel = (String) optional.get().getProperties().get("mediaChannel");
			oldFacility = optional.get();
		}
		
		if(newChannel != null && newChannel.length() > 0) {
			List<Facility> facilitys = facilityRepository.findFacilityByProperty("mediaChannel", newChannel);
			if(facilitys.size() > 1) return -3; 		// channel name is duplicated.
			else if(facilitys.size() == 1) {
				if(facilitys.get(0).getFcode() != facility.getFcode()) return -3;
			}
		}
		
		Facility entity = facilityRepository.save(facility);
		/* if(entity.getFacilityCode() > -1) return 0; */
		
		return -2;
	}
	
	public int removeFacility(String facilityFcode) {
		Optional<Facility> optional = facilityRepository.findById(facilityFcode);
		if(optional.isPresent()) {
			try {
				facilityRepository.delete(optional.get());
				return 0;
			} catch(Exception e) {
				return -2;
			}
		}
		return -1;
	}
	
	public List<Facility> getAllFacility() {
		return (List<Facility>)facilityRepository.findAll();
	}
	
	public Page<Facility> getAllFacility(Pageable pageable) {		
		return facilityRepository.findAll(pageable);
	}
	
	public List<Facility> getEnabledFacilitys() {		
		BooleanExpression be = QFacility.facility.enabled.eq(true);
		return (List<Facility>)facilityRepository.findAll(be);
	}
	
	//??????????????? ?????? paging
	public Page<Facility> getEnabledFacilitys(Pageable pageable) {
		BooleanExpression be = QFacility.facility.enabled.eq(true);
		return facilityRepository.findAll(be, pageable);
	}
	
	public List<Facility> getEnabledFacilityHasProperty(String attr) {
		return (List<Facility>)facilityRepository.findEnabledFacilityHasProperty(attr);
	}
	
	public List<Facility> getEnabledFacilitysByStatus(FacilityStatus status) {
		BooleanExpression be = QFacility.facility.status.eq(status).and(QFacility.facility.enabled.eq(true));
		return (List<Facility>)facilityRepository.findAll(be);
	}
	
	public List<Facility> getAirSensorFacility() {
		return (List<Facility>)facilityRepository.findAirSensorFacility();
	}
	
	public List<Facility> getFireCCTVFacility() {
		return (List<Facility>)facilityRepository.findFireCCTVFacility();
	}
	
	public Facility getFireCCTVFacility(int nVaId) {
		return facilityRepository.findFireCCTVFacility(nVaId);
	}
	
	public Facility getFacility(String mobiusId) {
		BooleanExpression be = QFacility.facility.mobiusId.eq(mobiusId);
		
		return facilityRepository.findFacilityByMobiusId(mobiusId);		
	}
	
	public List<Facility> getEnabledAirSensorFacility() {
		return (List<Facility>)facilityRepository.findEnabledAirSensorFacility();
	}
	
	public List<Facility> getNotiFacility() {
		return (List<Facility>)facilityRepository.findNotiFacility();
	}
	
	public List<Facility> getEnabledNotiFacility() {
		return (List<Facility>)facilityRepository.findEnabledNotiFacility();
	}
	
	public Facility getFacilityByMobiusId(String mobiusId) {
		BooleanExpression booleanExpression = QFacility.facility.mobiusId.eq(mobiusId);
		Optional<Facility> facility = facilityRepository.findOne(booleanExpression);
		if(facility.isPresent()) return facility.get();
		return null;
	}
	
	public Facility getFacilityByFacilityId(String facilityId) {
		Optional<Facility> facility = facilityRepository.findById(facilityId);
		if(facility.isPresent()) return facility.get();
		return null;
	}
	
	public List<Facility> getHavingFacility(Long id) {
		return facilityRepository.getHavingFacility(id);
	}
	
	
    public Map<String, String> getFacilitySensorMap() {
        Map<String, String> map = new HashMap<>();
        
        List<String> list = facilityRepository.findFacilitySensorMap();
        for(String str : list) {
            String[] spl = str.split(",");
            map.put(spl[1], spl[0]);
        }
        
        return map;
    }
}