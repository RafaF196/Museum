clear

I = imread('colorMuseumMap2.png');
[rows, columns, numberOfColorChannels] = size(I);

fileID = fopen('map2.txt','w');
fprintf(fileID,'%d\n%d\n',rows,columns);
for i = 1:rows
    for j = 1:columns
        if I(i,j,1) == 255 && I(i,j,2) == 255 && I(i,j,3) == 255
            fprintf(fileID,'-1\n');
        end
        if I(i,j,1) == 0 && I(i,j,2) == 0 && I(i,j,3) == 0
            fprintf(fileID,'0\n');
        end
        if I(i,j,1) == 255 && I(i,j,2) == 0 && I(i,j,3) == 0
            fprintf(fileID,'1\n');
        end
        if I(i,j,1) == 0 && I(i,j,2) == 255 && I(i,j,3) == 0
            fprintf(fileID,'2\n');
        end
        if I(i,j,1) == 0 && I(i,j,2) == 0 && I(i,j,3) == 255
            fprintf(fileID,'3\n');
        end
    end
end
fclose(fileID);