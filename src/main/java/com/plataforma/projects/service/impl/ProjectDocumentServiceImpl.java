package com.plataforma.projects.service.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.plataforma.projects.dto.DocumentUploadRequest;
import com.plataforma.projects.dto.DocumentUploadResponse;
import com.plataforma.projects.exception.ProjectNotFoundException;
import com.plataforma.projects.exception.UnauthorizedProjectAccessException;
import com.plataforma.projects.model.Project;
import com.plataforma.projects.model.ProjectDocument;
import com.plataforma.projects.repository.ProjectDocumentRepository;
import com.plataforma.projects.repository.ProjectRepository;
import com.plataforma.projects.service.ProjectDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectDocumentServiceImpl implements ProjectDocumentService {

    private final ProjectDocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public List<ProjectDocument> listDocuments(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
        return documentRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional
    public DocumentUploadResponse initiateUpload(Long projectId, DocumentUploadRequest request, Long requesterId, boolean isAdmin) {
        Project project = projectRepository.findByIdAndActiveTrue(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!isAdmin && !project.getOwnerId().equals(requesterId)) {
            throw new UnauthorizedProjectAccessException();
        }

        String s3Key = "projects/" + projectId + "/" + UUID.randomUUID() + "_" + request.getFileName();

        // Guardar el registro del documento antes de que el cliente suba el archivo
        ProjectDocument doc = ProjectDocument.builder()
                .project(project)
                .documentType(request.getDocumentType())
                .fileName(request.getFileName())
                .s3Key(s3Key)
                .mimeType(request.getMimeType())
                .fileSizeBytes(request.getFileSizeBytes())
                .build();
        ProjectDocument saved = documentRepository.save(doc);

        // Generar presigned URL para que el cliente suba directamente a S3
        Date expiration = new Date(System.currentTimeMillis() + 15 * 60 * 1000); // 15 min
        GeneratePresignedUrlRequest presignedRequest = new GeneratePresignedUrlRequest(bucket, s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        String presignedUrl = amazonS3.generatePresignedUrl(presignedRequest).toString();

        return DocumentUploadResponse.builder()
                .documentId(saved.getId())
                .presignedUrl(presignedUrl)
                .s3Key(s3Key)
                .documentType(request.getDocumentType())
                .fileName(request.getFileName())
                .build();
    }

    @Override
    @Transactional
    public void deleteDocument(Long projectId, Long documentId, Long requesterId, boolean isAdmin) {
        Project project = projectRepository.findByIdAndActiveTrue(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!isAdmin && !project.getOwnerId().equals(requesterId)) {
            throw new UnauthorizedProjectAccessException();
        }

        ProjectDocument doc = documentRepository.findByIdAndProjectId(documentId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        try {
            amazonS3.deleteObject(bucket, doc.getS3Key());
        } catch (Exception ignored) {
            // Si S3 falla en el delete, igual eliminamos el registro
        }
        documentRepository.delete(doc);
    }
}
